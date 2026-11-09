#version 120

/* DRAWBUFFERS:0 */

// Mây vanilla luôn trôi nên bị bỏ hẳn; mây đứng yên được vẽ trong gbuffers_skybasic.
void main() {
    discard;
}
