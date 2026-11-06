// Gió nhẹ làm hoa hướng dương / cỏ đung đưa trong Ký Ức Của Nam (world2).
// CHỈ được include từ shaders/world2/*.vsh, và phải include SAU "/lib/inception_curve.glsl"
// (dùng frameTimeCounter khai báo ở đó).
//
// Không có block.properties riêng cho world2, nên nhận diện cây cỏ bằng pháp tuyến:
// model chữ thập / mặt hoa nghiêng có pháp tuyến KHÔNG thẳng trục, còn block đặc thì thẳng trục.
// Độ lắc tăng theo độ cao so với mặt cỏ (y = 61 là gốc cây): gốc đứng yên, bông hoa lắc nhiều nhất.
#ifndef NAM_MEMORY_WIND_GLSL
#define NAM_MEMORY_WIND_GLSL

uniform vec3 cameraPosition;

#define NM_WIND_STRENGTH 0.09   // biên độ lắc (block) ở ngọn [0.0 0.05 0.09 0.14 0.2]
#define NM_WIND_SPEED    1.0    // [0.5 0.75 1.0 1.5]
#define NM_PLANT_BASE_Y  61.0   // mặt trên lớp cỏ của chiều này (cỏ ở y = 60)
#define NM_PLANT_HEIGHT  2.0    // hoa hướng dương cao 2 block

// playerPos  : vị trí đỉnh (chưa bẻ cong) trong không gian người chơi
// modelNormal: gl_Normal (với terrain, trục trùng trục thế giới)
// trả về độ dời cần cộng vào playerPos
vec3 nmWindOffset(vec3 playerPos, vec3 modelNormal) {
    vec3 a = abs(modelNormal);
    float axisAligned = max(a.x, max(a.y, a.z));
    float plant = 1.0 - smoothstep(0.88, 0.97, axisAligned);
    if (plant <= 0.0) return vec3(0.0);

    vec3 wp = playerPos + cameraPosition;
    float h = clamp((wp.y - NM_PLANT_BASE_Y) / NM_PLANT_HEIGHT, 0.0, 1.0);
    if (h <= 0.0) return vec3(0.0);
    h = h * h * (3.0 - 2.0 * h); // gốc cứng, ngọn mềm

    float t = frameTimeCounter * NM_WIND_SPEED;

    // Cơn gió lớn chậm lướt qua cánh đồng theo hướng chéo, cộng rung nhỏ riêng từng cây
    float gust = 0.55 + 0.45 * sin(t * 0.45 - dot(wp.xz, vec2(0.045, 0.030)));
    float phase = dot(wp.xz, vec2(0.37, 0.23));
    float swayMain = sin(t * 1.55 + phase);
    float swaySide = sin(t * 2.30 + phase * 1.7 + 1.3);

    vec2 windDir = normalize(vec2(0.8, 0.6));
    vec2 sideDir = vec2(-windDir.y, windDir.x);
    vec2 off = windDir * (swayMain * 0.75 + 0.35) * gust + sideDir * swaySide * 0.30;

    vec3 o = vec3(off.x, 0.0, off.y) * NM_WIND_STRENGTH * h * plant;
    o.y = -length(o.xz) * 0.25; // ngọn hơi chúi xuống khi nghiêng
    return o;
}

#endif
