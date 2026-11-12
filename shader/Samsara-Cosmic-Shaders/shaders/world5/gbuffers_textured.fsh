#version 120

/* DRAWBUFFERS:0 */

// Tầng 3 "Trốn tìm": mọi thứ có texture (khối, thực thể, tay, chữ, mây, nước...) đều đen tuyền.
// Ngoại lệ: thứ EchoRenderer vẽ với màu đỉnh g = b = 253/255 (dấu tiếng vọng). Khi đó hiện texture thật,
// ngả xám như một ký ức, độ sáng lấy từ kênh r, độ trong lấy từ alpha.

uniform sampler2D texture;

varying vec4 color;
varying vec2 texcoord;

const float ECHO_MARK = 253.0 / 255.0;

void main() {
    vec4 tex = texture2D(texture, texcoord);
    if (tex.a < 0.1) discard;
    bool echo = abs(color.g - ECHO_MARK) < 0.002 && abs(color.b - ECHO_MARK) < 0.002;
    if (!echo) {
        gl_FragData[0] = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    float gray = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
    vec3 col = mix(vec3(gray), tex.rgb, 0.45) * color.r * 1.15;
    gl_FragData[0] = vec4(col, tex.a * color.a);
}
