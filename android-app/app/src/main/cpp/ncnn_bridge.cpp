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

const int INPUT_SIZE = 2048;
const int NUM_CLASSES = 11;
const float CONF_THRESHOLD = 0.25f; // Obniżone dla pełnego testu
const float NMS_THRESHOLD = 0.45f;

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
    if (inter_w <= 0 || inter_h <= 0) return 0.0f;
    return inter_w * inter_h;
}

static bool compare_objects(const Object& a, const Object& b) {
    return a.prob > b.prob;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_przestrzeliny_1app_MainActivity_detectBulletHoles(JNIEnv *env, jobject thiz, jobject bitmap, jobject asset_manager) {

    LOGD("=== START DETEKCJI ===");

    if (!is_model_loaded) {
        AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
        yolo_model.opt.use_vulkan_compute = false; // Wyłączone dla stabilności
        yolo_model.opt.num_threads = 4;

        if (yolo_model.load_param(mgr, "model.ncnn.param") == 0 && yolo_model.load_model(mgr, "model.ncnn.bin") == 0) {
            is_model_loaded = true;
            LOGD("Model załadowany pomyślnie!");
        } else {
            LOGD("BŁĄD: Nie można załadować modelu!");
            return nullptr;
        }
    }

    AndroidBitmapInfo info;
    void* pixels = nullptr;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return nullptr;

    ncnn::Mat input_mat = ncnn::Mat::from_pixels((const unsigned char*)pixels, ncnn::Mat::PIXEL_RGBA2RGB, info.width, info.height);
    AndroidBitmap_unlockPixels(env, bitmap);

    ncnn::Mat resized_mat;
    ncnn::resize_bilinear(input_mat, resized_mat, INPUT_SIZE, INPUT_SIZE);
    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    resized_mat.substract_mean_normalize(0, norm_vals);

    LOGD("Inference start...");
    ncnn::Extractor ex = yolo_model.create_extractor();

    // UWAGA: Jeśli wywali aplikację tutaj, zmień "in0" na "data" lub "images"
    ex.input("in0", resized_mat);

    ncnn::Mat output_mat;
    // UWAGA: Jeśli wywali aplikację tutaj, zmień "out0" na "output"
    ex.extract("out0", output_mat);
    LOGD("Inference done. Output dims: W=%d, H=%d", output_mat.w, output_mat.h);

    std::vector<Object> proposals;
    int num_preds = output_mat.w;
    int num_attrs = output_mat.h;
    bool is_transposed = false;
    if (num_attrs > num_preds) {
        num_preds = output_mat.h;
        num_attrs = output_mat.w;
        is_transposed = true;
    }

    for (int i = 0; i < num_preds; i++) {
        float max_class_score = -1.0f;
        int best_class_id = -1;

        for (int c = 0; c < NUM_CLASSES; c++) {
            float score = is_transposed ? output_mat.row(i)[4 + c] : output_mat.row(4 + c)[i];
            if (score > max_class_score) {
                max_class_score = score;
                best_class_id = c;
            }
        }

        if (max_class_score > CONF_THRESHOLD) {
            LOGD("Znaleziono potencjalny strzał! Klasa: %d, Pewność: %f", best_class_id, max_class_score);
            Object obj;
            obj.x = is_transposed ? output_mat.row(i)[0] : output_mat.row(0)[i];
            obj.y = is_transposed ? output_mat.row(i)[1] : output_mat.row(1)[i];
            obj.w = is_transposed ? output_mat.row(i)[2] : output_mat.row(2)[i];
            obj.h = is_transposed ? output_mat.row(i)[3] : output_mat.row(3)[i];
            obj.label = best_class_id;
            obj.prob = max_class_score;
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

    jfloatArray result_array = env->NewFloatArray(picked.size() * 4);
    std::vector<float> data;
    for (size_t i = 0; i < picked.size(); i++) {
        const Object& obj = proposals[picked[i]];
        data.push_back(obj.x);
        data.push_back(obj.y);
        data.push_back((float)obj.label);
        data.push_back(obj.prob);
    }
    env->SetFloatArrayRegion(result_array, 0, data.size(), data.data());
    LOGD("=== KONIEC: Znaleziono %d obiektów ===", (int)picked.size());
    return result_array;
}