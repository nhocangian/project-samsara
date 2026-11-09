// Tầng Dưới (Đường Ống): ánh sáng block hơi lạnh kiểu đèn huỳnh quang.
// Lightmap vanilla ngả vàng làm gạch trắng ra màu be; giữ độ sáng, thay màu.
vec3 coolBlockLight(vec3 light) {
    float lum = dot(light, vec3(0.299, 0.587, 0.114));
    return lum * vec3(0.90, 0.97, 1.05);
}
