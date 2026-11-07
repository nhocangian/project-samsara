// Không khí The Womb (world1), dùng chung cho gbuffers_terrain / block / entities / water.
// Include SAU khi đã khai báo `uniform mat4 gbufferModelViewInverse;` (file này không khai báo lại).
//
//  - WOMB_LAMP: màu ánh sáng từ block (đuốc, đèn, block ánh sáng của mod). Trắng ngà, đứng yên.
//    (Bản cũ tô lửa linh hồn xanh lơ/tím đổi màu liên tục: nhuộm tím hồng thạch cao và cửa màu phấn.)
//  - Sương của The Womb nằm ở world1/composite.fsh (sương thật trong không gian, phủ cả bầu trời), không tô ở đây.

uniform vec3 cameraPosition;
const vec3 WOMB_LAMP = vec3(1.0, 0.92, 0.80);
