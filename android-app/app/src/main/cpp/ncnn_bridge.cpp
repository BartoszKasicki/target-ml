#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>
#include <net.h>
#include <cpu.h>
#include <vector>
#include <algorithm>

#define LOG_TAG "Most_AI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

ncnn::Net yolo_model;
bool is_model_loaded = false;

// Skoro model był trenowany na 2048
const int INPUT_SIZE = 2048;
const int NUM_CLASSES = 11;

const float CONF_THRESHOLD = 0.15f;// Próg 15% pewności modelu
const float NMS_THRESHOLD = 0.45f; // Próg 45% powielenia

struct Object {
    float x, y, w, h;
    int label;
    float prob;
};

static inline float intersection_area(const Object& a, const Object& b) {
    float inter_x = std::max(a.x - a.w / 2, b.x - b.w / 2);
    float inter_y = std::max(a.y - a.h / 2, b.y - b.h / 2);
    float inter_w = std::min(a.x + a.w / 2, b.x + b.w / 2) - inter_x;
    float inter_h = std::min(a.y + a.h / 2, b.y + b.h / 2) - inter_y;
    return (inter_w > 0 && inter_h > 0) ? (inter_w * inter_h) : 0.0f;
}

static bool compare_objects(const Object& a, const Object& b) {
    return a.prob > b.prob;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_przestrzeliny_1app_YoloDetector_initModel(JNIEnv *env, jobject thiz, jobject asset_manager, jstring param_path, jstring bin_path) {
    if (is_model_loaded) return JNI_TRUE;

    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    const char* param = env->GetStringUTFChars(param_path, 0);
    const char* bin = env->GetStringUTFChars(bin_path, 0);

    yolo_model.opt.use_vulkan_compute = false;
    yolo_model.opt.num_threads = 4;

    int ret_param = yolo_model.load_param(mgr, param);
    int ret_bin = yolo_model.load_model(mgr, bin);

    env->ReleaseStringUTFChars(param_path, param);
    env->ReleaseStringUTFChars(bin_path, bin);

    if (ret_param == 0 && ret_bin == 0) {
        is_model_loaded = true;
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_przestrzeliny_1app_YoloDetector_processImage(JNIEnv *env, jobject thiz, jobject bitmap) {
    if (!is_model_loaded) return nullptr;

    AndroidBitmapInfo info;
    void* pixels = nullptr;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return nullptr;

    ncnn::Mat input_mat = ncnn::Mat::from_pixels((const unsigned char*)pixels, ncnn::Mat::PIXEL_RGBA2BGR, info.width, info.height);AndroidBitmap_unlockPixels(env, bitmap);

    // AI pracuje na pełnej rozdzielczości 2048x2048
    ncnn::Mat resized_mat;
    ncnn::resize_bilinear(input_mat, resized_mat, INPUT_SIZE, INPUT_SIZE);
    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    resized_mat.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolo_model.create_extractor();
    ex.input("in0", resized_mat);
    ncnn::Mat output_mat;
    ex.extract("out0", output_mat);

    std::vector<Object> proposals;

    bool is_transposed = output_mat.h > output_mat.w;
    int num_anchors = is_transposed ? output_mat.h : output_mat.w;

    for (int i = 0; i < num_anchors; i++) {
        float max_score = -1.0f;
        int best_label = -1;

        for (int c = 0; c < NUM_CLASSES; c++) {
            float s = is_transposed ? output_mat.row(i)[4 + c] : output_mat.row(4 + c)[i];
            if (s > max_score) {
                max_score = s;
                best_label = c;
            }
        }

        if (max_score > CONF_THRESHOLD) {
            float raw_x = is_transposed ? output_mat.row(i)[0] : output_mat.row(0)[i];
            float raw_y = is_transposed ? output_mat.row(i)[1] : output_mat.row(1)[i];
            float raw_w = is_transposed ? output_mat.row(i)[2] : output_mat.row(2)[i];
            float raw_h = is_transposed ? output_mat.row(i)[3] : output_mat.row(3)[i];

            // Odrzucamy ewidentne śmieci z błędów pamięci (np. gigantyczne wymiary)
            if (raw_w < 2.0f || raw_h < 2.0f || raw_w > 500.0f || raw_h > 500.0f) continue;

            Object obj;
            obj.x = raw_x;
            obj.y = raw_y;
            obj.w = raw_w;
            obj.h = raw_h;
            obj.label = best_label;
            obj.prob = max_score;
            proposals.push_back(obj);
        }
    }

    std::sort(proposals.begin(), proposals.end(), compare_objects);
    std::vector<int> picked;
    for (size_t i = 0; i < proposals.size(); i++) {
        int keep = 1;
        for (size_t j = 0; j < picked.size(); j++) {
            float inter_area = intersection_area(proposals[i], proposals[picked[j]]);
            float union_area = (proposals[i].w * proposals[i].h) + (proposals[picked[j]].w * proposals[picked[j]].h) - inter_area;
            if (inter_area / union_area > NMS_THRESHOLD) { keep = 0; break; }
        }
        if (keep) picked.push_back(i);
    }

    // Wysyłamy obiekty do Javy
    jclass detClass = env->FindClass("com/example/przestrzeliny_app/Detection");
    jmethodID detInit = env->GetMethodID(detClass, "<init>", "(FFFFIF)V");
    jobjectArray detArray = env->NewObjectArray(picked.size(), detClass, NULL);

    for (size_t i = 0; i < picked.size(); i++) {
        const Object& obj = proposals[picked[i]];
        jobject detObj = env->NewObject(detClass, detInit, obj.x, obj.y, obj.w, obj.h, obj.label, obj.prob);
        env->SetObjectArrayElement(detArray, i, detObj);
        env->DeleteLocalRef(detObj);
    }

    return detArray;
}