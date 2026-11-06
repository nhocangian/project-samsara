// Bẻ cong thế giới kiểu "Inception" cho chiều Cánh Đồng Hướng Dương (samsara:nam_memory).
// Mặt đất phẳng bị uốn NGƯỢC LÊN theo bình phương khoảng cách ngang tới camera,
// nên đường chân trời cuộn lên như thành một cái bát khổng lồ. Độ cong "thở" rất chậm.
#ifndef INCEPTION_CURVE_GLSL
#define INCEPTION_CURVE_GLSL

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;

#define INCEPTION_CURVE 0.0045 // [0.0 0.0015 0.003 0.0045 0.006 0.008 0.012]

// Bẻ cong một điểm trong không gian người chơi (gốc = camera, trục theo thế giới).
// Dùng chung cho gbuffers và pass bóng (shadow) để bóng đổ khớp với hình học đã cong.
vec3 inceptionCurvePlayer(vec3 p) {
    float d2 = dot(p.xz, p.xz);
    float breathe = 1.0 + 0.08 * sin(frameTimeCounter * 0.35);
    p.y += d2 * INCEPTION_CURVE * breathe;
    return p;
}

vec4 inceptionCurve(vec4 viewPosition) {
    vec3 p = (gbufferModelViewInverse * viewPosition).xyz;
    p = inceptionCurvePlayer(p);
    return gbufferModelView * vec4(p, 1.0);
}

#endif
