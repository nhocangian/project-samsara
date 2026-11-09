// Đường Ống: sóng nhấp nhô như mặt sông, dùng chung cho phản chiếu và khúc xạ đáy.
// Trả về độ dốc của mặt nước theo (x, z) trong toạ độ thế giới.

const float CAUSTIC_PERIOD_A = 7.0;     // block
const float CAUSTIC_PERIOD_B = 11.3;    // block
const float CAUSTIC_SPEED    = 0.45;
const float CAUSTIC_STRENGTH = 0.75;

vec2 wave(vec2 p, float t, vec2 dir, float freq, float speed, float slope) {
    return dir * slope * cos(dot(dir, p) * freq + t * speed);
}

// Sóng chạy gần cùng một hướng như dòng chảy. Toạ độ được bẻ nhẹ để không thành các sọc đều.
vec2 waterSlope(vec2 p, float t) {
    vec2 q = p + 0.7 * vec2(sin(p.y * 0.37 + t * 0.21), cos(p.x * 0.29 - t * 0.17));
    vec2 s = wave(q, t, vec2( 0.60,  0.80), 0.55, 1.10, 0.0300);
    s     += wave(q, t, vec2( 0.26,  0.97), 1.30, 1.60, 0.0240);
    s     += wave(q, t, vec2( 0.91,  0.42), 2.10, 2.10, 0.0180);
    s     += wave(q, t, vec2(-0.31,  0.95), 4.30, 3.00, 0.0100);
    s     += wave(q, t, vec2( 0.97,  0.24), 6.90, 3.90, 0.0060);
    return s;
}

// Độ cao mép nước trên vách, từ 0 tới EDGE_HEIGHT block: làm đường tiếp xúc dập dềnh.
const float EDGE_HEIGHT = 0.09;

float waterEdgeHeight(vec2 p, float t) {
    vec2 q = p + 0.7 * vec2(sin(p.y * 0.37 + t * 0.21), cos(p.x * 0.29 - t * 0.17));
    float n = 0.50 * sin(dot(vec2(0.60, 0.80), q) * 1.30 + t * 1.60)
            + 0.30 * sin(dot(vec2(0.26, 0.97), q) * 2.70 + t * 2.10)
            + 0.20 * sin(dot(vec2(0.91, 0.42), q) * 5.30 + t * 3.00);
    return EDGE_HEIGHT * (0.5 + 0.5 * n);
}

// Bọt nước: mạng lưới bong bóng trôi theo dòng, tụ ven vách và rải thành mảng thưa ở giữa.
const float FOAM_STRENGTH = 0.55;
const float FOAM_PIXELS   = 16.0;    // số điểm ảnh mỗi block, khớp texture Minecraft
const float FOAM_SPEED    = 0.14;    // block mỗi giây

vec2 foamHash(vec2 p) {
    vec3 a = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    a += dot(a, a.yzx + 33.33);
    return fract((a.xx + a.yz) * a.zy);
}

float foamNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = foamHash(i).x;
    float b = foamHash(i + vec2(1.0, 0.0)).x;
    float c = foamHash(i + vec2(0.0, 1.0)).x;
    float d = foamHash(i + vec2(1.0, 1.0)).x;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Nhiễu đổi dần theo thời gian: mảng bọt sinh ra rồi tan đi thay vì chỉ trượt
float foamNoiseFading(vec2 p, float t, float period) {
    float time = t / period;
    float slice = floor(time);
    float a = foamNoise(p + slice * vec2(7.3, 3.1));
    float b = foamNoise(p + (slice + 1.0) * vec2(7.3, 3.1));
    return mix(a, b, smoothstep(0.0, 1.0, time - slice));
}

// Thành bong bóng. x = độ phủ (mỗi bong bóng tự hiện rồi tan theo nhịp riêng), y = độ sáng riêng của ô.
vec2 foamCells(vec2 p, float t) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float f1 = 8.0;
    float f2 = 8.0;
    vec2 nearest = vec2(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            vec2 h = foamHash(i + g);
            vec2 point = g + 0.5 + 0.38 * sin(t * 0.45 + h * 6.2831853);
            float d = length(point - f);
            if (d < f1) { f2 = f1; f1 = d; nearest = h; }
            else if (d < f2) { f2 = d; }
        }
    }
    float wall = 1.0 - smoothstep(0.02, 0.20, f2 - f1);
    float life = smoothstep(-0.25, 0.55, sin(t * (0.22 + 0.30 * nearest.x) + nearest.y * 6.2831853));
    return vec2(wall * life, nearest.y);
}

// nearEdge: 0 giữa dòng, 1 sát vách. Trả về x = độ phủ bọt 0..1, y = hệ số sáng.
vec2 waterFoam(vec2 worldXZ, float t, float dist, float nearEdge) {
    // Cùng chiều với waterSlope (sóng trôi về phía -flow), thêm chút cuộn để không trượt đều
    vec2 q = floor(worldXZ * FOAM_PIXELS) / FOAM_PIXELS + vec2(0.60, 0.80) * t * FOAM_SPEED;
    q += 0.28 * sin(q.yx * 0.60 + t * 0.20);

    float patches = smoothstep(0.58, 0.82, foamNoiseFading(q * 0.45 + 13.0, t, 9.0));
    float ragged = 0.40 + 0.60 * foamNoiseFading(q * 1.7, t, 6.0);
    float area = clamp(max(patches * 0.75, smoothstep(0.15, 0.85, nearEdge * ragged * 1.6)), 0.0, 1.0);
    if (area <= 0.0) return vec2(0.0, 1.0);

    // Cỡ bọt đổi theo từng vùng: to, vừa và li ti
    float sizeField = foamNoise(q * 0.8 + 41.0);
    vec2 big   = foamCells(q * 1.7, t);
    vec2 mid   = foamCells(q * 3.6 + 5.0, t * 1.2);
    vec2 small = foamCells(q * 8.0 + 9.0, t * 1.5);
    float wBig   = 1.0 - smoothstep(0.30, 0.55, sizeField);
    float wSmall = smoothstep(0.50, 0.75, sizeField);
    float wMid   = 1.0 - max(wBig, wSmall) * 0.65;
    float cells = clamp(big.x * wBig + mid.x * wMid * 0.8 + small.x * wSmall * 0.7, 0.0, 1.0);

    // Vùng sáng rộng giả ánh đèn phản chiếu, cộng vài bong bóng bắt sáng riêng lẻ
    float sheen = mix(0.62, 1.30, smoothstep(0.30, 0.75, foamNoiseFading(q * 0.9 + 23.0, t, 7.0)));
    float sparkle = smoothstep(0.80, 0.97, mid.y) * mid.x * 0.55 + smoothstep(0.84, 0.98, small.y) * small.x * 0.40;

    float fade = 1.0 - smoothstep(14.0, 40.0, dist);
    // Ở xa không còn thấy từng bong bóng, chỉ còn mảng trắng nhạt
    return vec2(area * mix(0.26, clamp(cells + 0.08, 0.0, 1.0), fade), sheen + sparkle * fade);
}

// Đèn trong ảnh: chỉ phần gần trắng hẳn, để gạch sáng không bị tính là đèn
float lampMask(vec3 c) {
    return smoothstep(0.86, 0.97, max(max(c.r, c.g), c.b));
}

// Vân sáng dưới đáy nước (caustic): ánh đèn bị mặt nước gợn hội tụ thành các đường mảnh.
// Một lớp, lặp lại sau mỗi `period` block.
float causticLayer(vec2 worldXZ, float period, float t) {
    const float TAU = 6.28318530718;
    vec2 p = mod(worldXZ * TAU / period, TAU) - 250.0;
    vec2 i = p;
    float c = 1.0;
    const float inten = 0.005;
    for (int n = 0; n < 5; n++) {
        float tt = t * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(tt - i.x) + sin(tt + i.y), sin(tt - i.y) + cos(tt + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + tt) / inten), p.y / (cos(i.y + tt) / inten)));
    }
    c /= 5.0;
    c = 1.17 - pow(c, 1.4);
    return clamp(pow(abs(c), 8.0), 0.0, 1.0);
}

// Hai lớp chu kỳ khác nhau để mắt không nhận ra chỗ lặp
float caustics(vec2 worldXZ, float t) {
    float a = causticLayer(worldXZ, CAUSTIC_PERIOD_A, t * CAUSTIC_SPEED);
    float b = causticLayer(worldXZ + vec2(3.7, 1.9), CAUSTIC_PERIOD_B, t * CAUSTIC_SPEED * 0.8 + 11.0);
    return clamp(a * 0.65 + b * 0.55, 0.0, 1.0);
}
