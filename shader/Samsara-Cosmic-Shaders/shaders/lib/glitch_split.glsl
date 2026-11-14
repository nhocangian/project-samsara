// Hiệu ứng "đồ vật không ổn định": rung nhẹ + nhoè màu glitch (tách ba kênh đỏ / lục / lam lệch nhau, độ lệch giật
// theo nhịp, thỉnh thoảng một dải ngang bị xô). Làm lần đầu cho vết rách tầng 4 (world6), dùng lại cho đồ đạc phòng
// ngủ ở Bến Đò (world1). Các program gbuffers ghi mặt nạ vào colortex5.r; final gọi glitchSplit().

float glitchHash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Đợt giật: phần lớn thời gian 0, cứ vài giây lại có một cú giật ngắn (~0,4 s) lên 1.
float glitchBurst(float t) {
    float cycle = floor(t / 4.5);
    float start = glitchHash(vec2(cycle, 9.1)) * 3.6;
    float local = t - cycle * 4.5 - start;
    return (local > 0.0 && local < 0.4) ? 1.0 : 0.0;
}

// Độ xê dịch đỉnh khối (khối), vài sóng nhanh lệch tần số, biên độ cỡ 1/30 khối khi strength = 1.
vec3 glitchShake(float t, float strength) {
    return vec3(sin(t * 37.0) + 0.6 * sin(t * 71.0 + 1.3),
                sin(t * 43.0 + 2.1) + 0.6 * sin(t * 67.0),
                sin(t * 31.0 + 0.7) + 0.6 * sin(t * 79.0 + 2.9)) * 0.018 * strength;
}

// Màu cảnh ở uv sau khi nhoè glitch những chỗ có mặt nạ (mask.r > 0.5). Viền màu được tràn ra sát ngoài một chút.
// strength: 1 = như vết rách tầng 4; nhỏ hơn thì lệch ít hơn.
vec3 glitchSplit(sampler2D scene, sampler2D mask, vec2 uv, float t, float strength) {
    vec3 base = texture2D(scene, uv).rgb;
    float tick = floor(t * 12.0);
    float kick = glitchHash(vec2(tick, 3.7));
    float amount = (0.0035 + (kick > 0.75 ? (kick - 0.75) * 0.05 : 0.0)) * strength;
    float band = floor(uv.y * 40.0);
    float slip = glitchHash(vec2(band, tick));
    vec2 suv = uv;
    if (slip > 1.0 - 0.07 * strength) suv.x += (slip - (1.0 - 0.07 * strength)) * 0.35;
    vec2 off = vec2(amount, amount * 0.25 * sin(t * 23.0));
    float mHere = texture2D(mask, suv).r;
    float mR = texture2D(mask, suv + off).r;
    float mB = texture2D(mask, suv - off).r;
    if (mHere + mR + mB < 0.5) return base;
    vec3 c = mHere > 0.5 ? texture2D(scene, suv).rgb : base;
    if (mR > 0.5 || mHere > 0.5) c.r = texture2D(scene, suv + off).r;
    if (mB > 0.5 || mHere > 0.5) c.b = texture2D(scene, suv - off).b;
    return c;
}
