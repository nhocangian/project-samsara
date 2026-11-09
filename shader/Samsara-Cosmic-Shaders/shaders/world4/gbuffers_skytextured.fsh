#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;

varying vec4 color;
varying vec2 texCoord;

void main() {
    vec4 texColor = texture2D(tex, texCoord);

    // Mặt trời vuông của game: bỏ đi, mặt trời tròn được vẽ trong ldnSky.
    // Tâm sáng và alpha đặc chỉ có ở sun.png gốc; các ảnh Hố Đen tâm đen hoặc alpha thấp hơn nên vẫn hiện.
    vec4 center = texture2D(tex, vec2(0.5));
    if (center.a > 0.97 && dot(center.rgb, vec3(0.299, 0.587, 0.114)) > 0.5) discard;

    // KHA'I PHC CHO MT TRNG (MOON PASS-THROUGH):
    // Texture Mt Trng c a Minecraft (moon_phases.png) hoAn toAn lA `n s_c xAm/tr_ng (|R - B| < 0.06).
    if (texColor.a > 0.05 && abs(texColor.r - texColor.b) < 0.06 && texColor.r > 0.25) {
        gl_FragData[0] = texColor * color;
        return;
    }

    // Xut texture vi hAo quang A'm sAt hAnh kh`i thc th (Texture-Contour Bloom)
    // HoAn toAn khA'ng cA qu ng trAn nhn to hay tia nhn
    float lum = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 outRgb = texColor.rgb;
    if (lum > 0.35 && texColor.a > 0.6) {
        outRgb = mix(texColor.rgb, vec3(1.0, 0.98, 0.90), (lum - 0.35) * 0.35);
    }

    gl_FragData[0] = vec4(outRgb, texColor.a) * color;
}
