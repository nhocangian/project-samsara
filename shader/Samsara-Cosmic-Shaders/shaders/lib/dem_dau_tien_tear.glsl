// Tầng 4 "Đêm đầu tiên": vết rách thời gian. Đảo là bản Alpha (texture lấy từ atlas specular, xem
// mod/tools/generate_alpha_textures.py); trong vết rách lộ bản 1.20.1 thật.
// Tâm và bán kính PHẢI khớp DemDauTienBuilder.TEARS bên mod.
uniform float frameTimeCounter;
uniform int worldTime;
uniform int worldDay;

// Giờ trong ngày của tầng (tick). Mod tự giữ giờ cho chiều này (DemDauTienClock) và trả cho client, nên nhịp thở của
// vết rách đi theo giờ này để server tính cùng một công thức, biết chỗ nào đang nằm trong vết rách.
// Mọi công thức dùng giờ này PHẢI khớp DemDauTienClock.margin bên mod.
float tearDayTime() {
    return float(worldDay) * 24000.0 + float(worldTime);
}

// Số ngày của đồng hồ là bitmask ba con cừu đã bị đánh (DemDauTienClock.sheepBits); giờ trong ngày chạy như thường.
float tearHour(float dayTime) {
    return mod(dayTime, 24000.0);
}

bool tearSheepHit(float dayTime, int k) {
    float bits = floor(dayTime / 24000.0);
    return mod(floor(bits / pow(2.0, float(k))), 2.0) > 0.5;
}

// Chỗ đứng (x, z) của con cừu thứ k: PHẢI khớp DemDauTienBuilder.SHEEP.
vec2 sheepSpot(int k) {
    if (k == 0) return vec2(6.5, 8.5);
    if (k == 1) return vec2(-6.5, 5.5);
    return vec2(9.5, -5.5);
}

// Vết rách nhỏ ở con cừu thứ k (đã bị đánh): ống đứng bán kính ~1 khối, mép sóng, trôi vòng quanh chỗ con cừu đứng
// nên cứ quét ngang qua người nó (chỗ vết rách phủ thì thấy còn lông). Giống DemDauTienClock.sheepMargin.
float sheepMargin(int k, vec3 p, float t) {
    if (p.y < 58.0 || p.y > 72.0) return -1e3;
    float fk = float(k);
    float orbit = t * 0.7 + fk * 2.1;
    vec2 d = p.xz - (sheepSpot(k) + 0.9 * vec2(cos(orbit), sin(orbit)));
    float dist = length(d);
    float a = atan(d.y, d.x);
    float r = 1.0 * (0.85 + 0.2 * sin(t * 0.9 + fk * 1.7)) + 0.15 * sin(a * 4.0 - t * 1.6 + fk)
            + 0.1 * sin(a * 7.0 + t * 1.1 + fk * 2.0);
    return r - dist;
}

// Cảnh cuối (giờ >= 18000): vết rách nở ra nuốt cả đảo, nhanh dần (khối cộng thêm vào bán kính).
float tearFinaleGrowth(float dayTime) {
    if (dayTime < 18000.0) return 0.0;
    float s = (dayTime - 18000.0) / 20.0;
    return s * s * 1.6;
}

const int TEAR_COUNT = 3;
vec4 tear(int i) {
    if (i == 0) return vec4(18.5, 68.0, -9.5, 6.5);    // cây anh đào
    if (i == 1) return vec4(-15.5, 67.0, 12.5, 6.0);   // góc nhà có đèn lồng
    return vec4(3.5, 66.5, 23.5, 4.5);                 // bụi đỗ quyên, dương xỉ
}

float tearHash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float tearNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(tearHash(i), tearHash(i + vec3(1, 0, 0)), f.x),
                  mix(tearHash(i + vec3(0, 1, 0)), tearHash(i + vec3(1, 1, 0)), f.x), f.y);
    float b = mix(mix(tearHash(i + vec3(0, 0, 1)), tearHash(i + vec3(1, 0, 1)), f.x),
                  mix(tearHash(i + vec3(0, 1, 1)), tearHash(i + vec3(1, 1, 1)), f.x), f.y);
    return mix(a, b, f.z);
}

// Nhịp thở của vết rách i (0..1): hai sóng chậm lệch chu kỳ nên không đều tăm tắp.
float tearBreath(int i, float t) {
    float fi = float(i);
    return 0.5 + 0.32 * sin(t * 0.55 + fi * 2.1) + 0.18 * sin(t * 1.37 + fi * 4.3);
}

// Rung nhẹ cả vùng rách cho cảm giác không ổn định (vài sóng nhanh lệch tần số, biên độ cỡ 1/30 khối).
vec3 tearShake(float t) {
    return vec3(sin(t * 37.0) + 0.6 * sin(t * 71.0 + 1.3),
                sin(t * 43.0 + 2.1) + 0.6 * sin(t * 67.0),
                sin(t * 31.0 + 0.7) + 0.6 * sin(t * 79.0 + 2.9)) * 0.018;
}

// Ngưỡng dither có thứ tự (Bayer 4x4), 0..1.
float bayer4(vec2 p) {
    vec2 q = mod(floor(p), 4.0);
    int i = int(q.x + q.y * 4.0);
    float m[16];
    m[0] = 0.0;  m[1] = 8.0;  m[2] = 2.0;  m[3] = 10.0;
    m[4] = 12.0; m[5] = 4.0;  m[6] = 14.0; m[7] = 6.0;
    m[8] = 3.0;  m[9] = 11.0; m[10] = 1.0; m[11] = 9.0;
    m[12] = 15.0; m[13] = 7.0; m[14] = 13.0; m[15] = 5.0;
    float v = 0.0;
    for (int k = 0; k < 16; k++) if (k == i) v = m[k];
    return (v + 0.5) / 16.0;
}

// Khoảng cách (khối) từ điểm này vào trong mép vết rách: dương là ở trong. Mép như bờ sóng biển: những con sóng chạy
// vòng theo mép, vỗ lấn vào rồi rút ra; cả vết rách "thở" nở ra co lại mượt (lúc co sát vào công trình thì lộ bản cũ
// của nó). Người dùng chốt: không nhảy cóc; bỏ dạng splash, mép như sóng biển.
float tearMargin(vec3 p, float dayTime) {
    float hour = tearHour(dayTime);
    float t = hour / 20.0;
    float grow = tearFinaleGrowth(hour);
    float margin = -1e3;
    for (int k = 0; k < 3; k++) {
        if (tearSheepHit(dayTime, k)) margin = max(margin, sheepMargin(k, p, t));
    }
    for (int i = 0; i < TEAR_COUNT; i++) {
        float fi = float(i);
        float R = tear(i).w;
        float b = 0.6 + 0.5 * tearBreath(i, t);
        vec3 d = (p - tear(i).xyz) * vec3(1.0, 1.25, 1.0);
        float dist = length(d);
        float a = atan(d.z, d.x);
        // Sóng chạy dọc mép (hai lớp ngược chiều) + đợt vỗ vào rút ra lệch pha theo từng đoạn bờ
        float waves = 0.55 * sin(a * 5.0 - t * 1.1 + fi) + 0.35 * sin(a * 3.0 + t * 0.7 + fi * 2.0);
        float surge = 0.6 * sin(t * 1.3 + sin(a * 2.0 + fi) * 2.2);
        float r = R * 0.85 * b + waves + surge + grow + (tearNoise(p * 0.5 + fi * 13.0) - 0.5) * 0.6;
        margin = max(margin, r - dist);
    }
    return margin;
}

// 1.0 = điểm này nằm trong vết rách. Rìa là một dải dither Bayer 4x4 hạt 1/32 khối (mịn gấp đôi pixel texture), hẹp
// và đặc để bám sát vết rách: đi từ trong ra, hạt bản mới thưa dần như bọt sóng.
// Người dùng chốt: dither đừng rời rạc, dày và mịn hơn nhưng không loang xa vết rách.
const float DITHER_BAND = 0.6;

float tearMask(vec3 worldPos) {
    vec3 texel = floor(worldPos * 32.0);
    float margin = tearMargin((texel + 0.5) / 32.0, tearDayTime());
    float level = clamp(margin / DITHER_BAND + 0.6, 0.0, 1.0);
    vec2 cell = vec2(texel.x + texel.y, texel.z + texel.y);
    return level > bayer4(cell) ? 1.0 : 0.0;
}

// Mức rung của đỉnh khối: chỉ những gì nằm hẳn trong vết rách mới rung, ra tới rìa thì tắt (người dùng chốt).
float tearShakeWeight(vec3 worldPos) {
    return clamp((tearMargin(worldPos, tearDayTime()) - DITHER_BAND * 0.5) / 1.5, 0.0, 1.0);
}
