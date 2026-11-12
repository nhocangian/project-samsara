#version 120

/* DRAWBUFFERS:0 */

// Tầng 3 "Trốn tìm": mọi thứ có texture (khối, thực thể, tay, trời, mây, nước...) đều đen tuyền.
// Chỉ tiếng vọng (vẽ bằng position_color, đi qua gbuffers_basic) mới sáng.

void main() {
    gl_FragData[0] = vec4(0.0, 0.0, 0.0, 1.0);
}
