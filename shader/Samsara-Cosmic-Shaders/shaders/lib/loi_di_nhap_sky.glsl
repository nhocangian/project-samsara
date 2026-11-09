// Lối đi nháp (world4): trời hoàng hôn hồng, mây thể tích mềm như sương.
// Mây không trôi: vị trí các cụm cố định trong thế giới, chỉ có lớp xốp bên trong cuộn chậm tại chỗ.
// Các màu ở đây còn đi qua đường cong phơi sáng trong composite.fsh nên sẽ sáng hơn trên màn hình.

const vec3  LDN_HORIZON = vec3(0.95, 0.60, 0.52);
const vec3  LDN_MIDSKY  = vec3(0.74, 0.43, 0.60);
const vec3  LDN_ZENITH  = vec3(0.22, 0.24, 0.56);
const vec3  LDN_CLOUD_LIT    = vec3(0.98, 0.82, 0.86);   // đỉnh mây hồng trắng
const vec3  LDN_CLOUD_MID    = vec3(0.96, 0.54, 0.66);   // thân mây hồng
const vec3  LDN_CLOUD_SHADOW = vec3(0.50, 0.63, 0.88);   // chỗ khuất xanh baby
const vec3  LDN_SUN_DIR = vec3(0.50, 0.75, 0.43);        // hướng nắng riêng cho mây
// Mặt trời tròn thấp phía biển; vật thể lấy nắng từ đây.
// Phải khớp với sunPathRotation trong gbuffers_terrain.fsh (z = -sin góc, y = cos góc) để bóng đổ đúng hướng.
const vec3  LDN_SUN_POS = vec3(0.0, 0.208, -0.978);

const float LDN_CLOUD_BASE = 115.0;       // độ cao đáy thấp nhất của cụm mây (mặt biển ở Y = 64)
const float LDN_CLOUD_BASE_SPREAD = 35.0; // các cụm cao thấp lệch nhau trong khoảng này
const float LDN_CLOUD_CELL = 440.0;       // mỗi ô lưới có tối đa một cụm
const float LDN_CLOUD_DENSITY = 0.62;     // tỉ lệ ô có mây: tăng thì nhiều mây hơn
const float LDN_CLOUD_R_MIN = 34.0;
const float LDN_CLOUD_R_MAX = 115.0;
const float LDN_CLOUD_FAR = 2200.0;
const float LDN_CLOUD_OPACITY = 7.0;      // tăng thì mây đặc hơn, giảm thì mỏng như khói
const int   LDN_CLOUD_STEPS = 26;         // số lớp lấy mẫu mỗi cụm: giảm nếu FPS thấp
const float LDN_CLOUD_BOIL_CYCLES = 16.0; // độ nhanh của mây cuộn: số vòng trong một giờ, phải là số nguyên
const float LDN_CLOUD_BOIL_SIZE = 0.9;    // biên độ mây cuộn

float ldnHash(vec2 p) {
    vec3 a = fract(vec3(p.xyx) * 0.1031);
    a += dot(a, a.yzx + 33.33);
    return fract((a.x + a.y) * a.z);
}

float ldnNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(ldnHash(i), ldnHash(i + vec2(1.0, 0.0)), f.x),
               mix(ldnHash(i + vec2(0.0, 1.0)), ldnHash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float ldnHash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float ldnNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(ldnHash3(i), ldnHash3(i + vec3(1.0, 0.0, 0.0)), f.x),
                  mix(ldnHash3(i + vec3(0.0, 1.0, 0.0)), ldnHash3(i + vec3(1.0, 1.0, 0.0)), f.x), f.y);
    float b = mix(mix(ldnHash3(i + vec3(0.0, 0.0, 1.0)), ldnHash3(i + vec3(1.0, 0.0, 1.0)), f.x),
                  mix(ldnHash3(i + vec3(0.0, 1.0, 1.0)), ldnHash3(i + vec3(1.0, 1.0, 1.0)), f.x), f.y);
    return mix(a, b, f.z);
}

float ldnFbm(vec3 p) {
    mat3 turn = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);
    float total = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        total += amp * ldnNoise3(p);
        p = turn * p * 2.02;
        amp *= 0.5;
    }
    return total / 0.9375;
}

vec3 ldnGradient(vec3 dir) {
    float up = clamp(dir.y, 0.0, 1.0);
    vec3 low = mix(LDN_HORIZON, LDN_MIDSKY, smoothstep(0.0, 0.28, up));
    return mix(low, LDN_ZENITH, smoothstep(0.20, 0.85, up));
}

// Thông tin cụm mây của một ô: xyz = gốc, w = bán kính, trả về false nếu ô trống.
// Cụm luôn nằm trọn trong ô của nó, nên không bao giờ bị cắt ở ranh giới ô.
bool ldnCluster(vec2 cell, out vec4 c, out float angle) {
    c = vec4(0.0);
    angle = ldnHash(cell + 41.7) * 6.2832;
    if (ldnHash(cell + 0.5) > LDN_CLOUD_DENSITY) return false;
    // Đa số cụm vừa và nhỏ, thỉnh thoảng có một cụm khổng lồ
    float r = mix(LDN_CLOUD_R_MIN, LDN_CLOUD_R_MAX, pow(ldnHash(cell + 13.1), 2.0));
    float room = LDN_CLOUD_CELL * 0.5 - r * 1.85;
    vec2 offset = (vec2(ldnHash(cell + 3.3), ldnHash(cell + 7.9)) - 0.5) * 2.0 * room;
    vec2 center = (cell + 0.5) * LDN_CLOUD_CELL + offset;
    c = vec4(center.x, LDN_CLOUD_BASE + ldnHash(cell + 23.7) * LDN_CLOUD_BASE_SPREAD, center.y, r);
    return true;
}

float ldnSmin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// Dáng thô của cụm: số âm là bên trong
float ldnCloudShape(vec3 q) {
    float d = length(q - vec3(0.00, 0.55, 0.00)) - 0.75;
    d = ldnSmin(d, length(q - vec3(0.70, 0.30, 0.20)) - 0.55, 0.25);
    d = ldnSmin(d, length(q - vec3(-0.65, 0.30, -0.30)) - 0.60, 0.25);
    d = ldnSmin(d, length(q - vec3(0.10, 1.15, 0.15)) - 0.50, 0.25);
    return ldnSmin(d, length(q - vec3(-0.20, 0.25, 0.75)) - 0.50, 0.25);
}

// Mật độ mây tại p, từ 0 (trống) đến 1 (đặc)
float ldnCloudDensity(vec3 p, vec4 c, float angle, vec3 boil) {
    vec3 q = (p - c.xyz) / c.w;
    float ca = cos(angle), sa = sin(angle);
    q.xz = vec2(ca * q.x - sa * q.z, sa * q.x + ca * q.z);

    if (ldnCloudShape(q) > 1.0) return 0.0;

    // Bẻ méo dáng thô để cụm không còn tròn
    vec3 seed = c.xzy * 0.0137 + boil * 0.6;
    vec3 warp =vec3(ldnNoise3(q * 1.4 + seed), ldnNoise3(q * 1.4 + seed + 19.1), ldnNoise3(q * 1.4 + seed + 37.7)) - 0.5;
    float shape = ldnCloudShape(q + warp * 0.5);

    float n = ldnFbm(q * 3.2 + seed + boil * 0.4);
    float density = smoothstep(0.0, 0.28, -shape + (n - 0.5) * 0.9);
    density *= smoothstep(-0.4, 0.05, q.y + (n - 0.5) * 0.2);    // đáy mây bằng hơn đỉnh
    density *= 1.0 - smoothstep(1.55, 1.78, length(q - vec3(0.0, 0.7, 0.0)));    // tan trước khi chạm biên
    return density;
}

// Màu trời theo hướng dir khi nhìn từ điểm origin (toạ độ thế giới)
vec3 ldnSky(vec3 origin, vec3 dir, float time) {
    vec3 sky = ldnGradient(dir);
    if (dir.y < 0.012) return sky;

    // Mặt trời tròn và quầng sáng, nằm sau mây
    float toSun = max(dot(dir, normalize(LDN_SUN_POS)), 0.0);
    sky += vec3(1.0, 0.76, 0.50) * (pow(toSun, 400.0) * 0.35 + pow(toSun, 30.0) * 0.14);
    sky = mix(sky, vec3(1.0, 0.96, 0.85), smoothstep(0.9985, 0.9991, toSun));

    // Dao động tròn theo chu kỳ một giờ, để lúc bộ đếm thời gian quay về 0 mây không bị giật
    float w = LDN_CLOUD_BOIL_CYCLES * 6.2832 / 3600.0;
    vec3 boil = vec3(sin(time * w), cos(time * w * 0.75), sin(time * w * 1.25 + 1.7)) * LDN_CLOUD_BOIL_SIZE;

    float yLow = LDN_CLOUD_BASE - LDN_CLOUD_R_MAX * 0.6;
    float yHigh = LDN_CLOUD_BASE + LDN_CLOUD_BASE_SPREAD + LDN_CLOUD_R_MAX * 2.0;
    float t = max(0.0, (yLow - origin.y) / dir.y);
    float tEnd = min((yHigh - origin.y) / dir.y, LDN_CLOUD_FAR);
    if (t >= tEnd) return sky;

    vec2 safeDir = vec2(abs(dir.x) < 1e-4 ? 1e-4 : dir.x, abs(dir.z) < 1e-4 ? 1e-4 : dir.z);
    vec3 sun = normalize(LDN_SUN_DIR);
    float jitter = ldnHash(gl_FragCoord.xy);    // lệch nhẹ để không thấy các lớp lấy mẫu
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // Đi qua các ô lưới từ gần ra xa, cộng dồn mây từng lớp
    for (int cellStep = 0; cellStep < 16 && t < tEnd && alpha < 0.97; cellStep++) {
        vec3 entry = origin + dir * t;
        vec2 cell = floor(entry.xz / LDN_CLOUD_CELL);
        vec2 next = (cell + step(vec2(0.0), safeDir)) * LDN_CLOUD_CELL;
        vec2 crossing = (next - origin.xz) / safeDir;
        float tExit = min(min(crossing.x, crossing.y), tEnd);

        vec4 c;
        float angle;
        if (ldnCluster(cell, c, angle)) {
            vec3 toOrigin = origin - (c.xyz + vec3(0.0, 0.7 * c.w, 0.0));
            float radius = 1.8 * c.w;
            float b = dot(toOrigin, dir);
            float disc = b * b - (dot(toOrigin, toOrigin) - radius * radius);
            if (disc > 0.0) {
                float root = sqrt(disc);
                float t0 = max(-b - root, t);
                float t1 = min(-b + root, tExit);
                if (t1 > t0) {
                    float stepLen = (t1 - t0) / float(LDN_CLOUD_STEPS);
                    for (int i = 0; i < LDN_CLOUD_STEPS && alpha < 0.97; i++) {
                        float tm = t0 + (float(i) + jitter) * stepLen;
                        vec3 p = origin + dir * tm;
                        float density = ldnCloudDensity(p, c, angle, boil);
                        if (density < 0.01) continue;

                        // Nắng xuyên qua: càng nhiều mây chắn phía mặt trời càng tối
                        float blockNear = ldnCloudDensity(p + sun * c.w * 0.22, c, angle, boil);
                        float blockFar = ldnCloudDensity(p + sun * c.w * 0.55, c, angle, boil);
                        float light = exp(-(blockNear * 1.3 + blockFar * 0.8) * 1.8);
                        light = clamp(light + clamp((p.y - c.y) / (c.w * 1.8), 0.0, 1.0) * 0.15, 0.0, 1.0);

                        vec3 cloud = mix(mix(LDN_CLOUD_SHADOW, LDN_CLOUD_MID, smoothstep(0.08, 0.45, light)),
                                         LDN_CLOUD_LIT, smoothstep(0.45, 0.90, light));
                        cloud = mix(cloud, sky, smoothstep(700.0, LDN_CLOUD_FAR, tm) * 0.5);

                        float a = 1.0 - exp(-density * stepLen / c.w * LDN_CLOUD_OPACITY);
                        color += cloud * a * (1.0 - alpha);
                        alpha += a * (1.0 - alpha);
                    }
                }
            }
        }
        t = max(tExit, t) + 0.05;
    }
    return sky * (1.0 - alpha) + color;
}

// Bản mềm cho mặt nước: chỉ giữ vệt hồng loang đúng vị trí các cụm mây, không có viền
vec3 ldnSkySoft(vec3 origin, vec3 dir) {
    vec3 sky = ldnGradient(dir);
    if (dir.y < 0.012) return sky;
    vec2 hit = origin.xz + dir.xz * ((LDN_CLOUD_BASE + 40.0 - origin.y) / dir.y);
    vec2 base = floor(hit / LDN_CLOUD_CELL);

    float cover = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec4 c;
            float angle;
            if (ldnCluster(base + vec2(float(x), float(y)), c, angle)) {
                cover = max(cover, 1.0 - smoothstep(c.w * 0.3, c.w * 1.7, length(hit - c.xz)));
            }
        }
    }
    return mix(sky, LDN_CLOUD_LIT, cover * smoothstep(0.012, 0.15, dir.y) * 0.75);
}
