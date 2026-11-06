#version 120

/* DRAWBUFFERS:0 */

// Mặt trời / mặt trăng. Đẩy độ sáng mặt trời lên vùng HDR để bloom tạo quầng chói.
// Mặt trời hố đen của mod là texture premultiplied (blend ONE, ONE_MINUS_SRC_ALPHA):
// chỉ nhân RGB, giữ nguyên alpha, nên công thức blend vẫn đúng.

#define NM_SUN_TEXTURE_BOOST 2.6 // [1.0 1.8 2.6 3.5]

uniform sampler2D tex;

varying vec4 color;
varying vec2 texCoord;

void main() {
    vec4 texColor = texture2D(tex, texCoord);

    // MẶT TRĂNG: texture moon_phases.png xám/trắng (|R - B| < 0.06) -> giữ nguyên
    if (texColor.a > 0.05 && abs(texColor.r - texColor.b) < 0.06 && texColor.r > 0.25) {
        gl_FragData[0] = texColor * color;
        return;
    }

    // Hào quang ôm sát hình khối (không vẽ quầng tròn nhân tạo hay tia sáng)
    float lum = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 outRgb = texColor.rgb;
    if (lum > 0.35 && texColor.a > 0.6) {
        outRgb = mix(texColor.rgb, vec3(1.0, 0.95, 0.82), (lum - 0.35) * 0.35);
    }
    outRgb *= NM_SUN_TEXTURE_BOOST;

    gl_FragData[0] = vec4(outRgb, texColor.a) * color;
}
