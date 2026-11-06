#version 120

/* DRAWBUFFERS:0 */

varying vec4 vertexPos;

uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjectionInverse;
uniform float frameTimeCounter;

// =========================================================================
// 1. NOISE THỦ TỤC & BẦU TRỜI SAO ĐẲNG HƯỚNG 100% (ISOTROPIC, KHÔNG KÉO DÃN)
// =========================================================================

float hash13(vec3 p) {
    vec3 p3 = fract(p * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec4 hash43(vec3 p) {
    vec4 p4 = fract(vec4(p.xyzx) * vec4(0.1031, 0.1030, 0.0973, 0.1099));
    p4 += dot(p4, p4.wzxy + 33.33);
    return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);

    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);

    return mix(y0, y1, f.z);
}

// Khói bụi fractal 3D mượt mà tự nhiên, không tạo vân sọc giả
float fbm3D(vec3 p) {
    float v = 0.55 * noise3D(p);
    v += 0.30 * noise3D(p * 2.18 + vec3(1.3, 0.7, 2.4));
    v += 0.15 * noise3D(p * 4.50 + vec3(3.1, 2.2, 1.1));
    return v;
}

// Bầu trời sao chuẩn thiên văn 100% đẳng hướng (triệt tiêu hoàn toàn vệt sọc/kéo dãn ở rìa)
vec3 sampleStarsClean(vec3 dir, float t) {
    vec3 col = vec3(0.0);

    // Tầng 1: Sao li ti dày đặc
    vec3 p1 = dir * 160.0;
    vec3 c1 = floor(p1);
    vec4 h1 = hash43(c1);
    if (h1.x > 0.965) {
        float d1 = length(p1 - (c1 + h1.yzw));
        float star1 = clamp(1.0 - d1 / 0.36, 0.0, 1.0);
        star1 *= star1;
        float twinkle = 0.75 + 0.35 * sin(h1.z * 60.0 + t * 2.2);
        vec3 tint1 = mix(vec3(1.0, 0.65, 0.30), vec3(0.75, 0.88, 1.0), h1.y);
        col += tint1 * star1 * twinkle * 1.8;
    }

    // Tầng 2: Sao sáng rực rỡ có màu phổ
    vec3 p2 = dir * 85.0;
    vec3 c2 = floor(p2);
    vec4 h2 = hash43(c2 + vec3(17.3, 31.7, 53.1));
    if (h2.x > 0.980) {
        float d2 = length(p2 - (c2 + h2.yzw));
        float star2 = clamp(1.0 - d2 / 0.40, 0.0, 1.0);
        star2 *= star2;
        float twinkle2 = 0.80 + 0.30 * sin(h2.w * 80.0 + t * 3.0);
        vec3 tint2 = mix(vec3(1.0, 0.85, 0.50), vec3(0.70, 0.85, 1.0), h2.y);
        col += tint2 * star2 * twinkle2 * 2.6;
    }

    return col;
}

// Phổ nhiệt bức xạ vật đen Planckian
vec3 KelvinToRgb(float Kelvin) {
    if (Kelvin < 400.0) return vec3(0.0);
    float Teff = (Kelvin - 6500.0) / (6500.0 * Kelvin * 2.2);
    vec3 col;
    col.r = exp(2.05539304e4 * Teff);
    col.g = exp(2.63463675e4 * Teff);
    col.b = exp(3.30145739e4 * Teff);
    float bScale = 1.0 / max(max(1.5 * col.r, col.g), col.b);
    if (Kelvin < 1000.0) bScale *= (Kelvin - 400.0) / 600.0;
    return col * bScale;
}

// Tone mapping ACES chuẩn điện ảnh
vec3 aces_tonemap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    // 1. Tái cấu trúc tia nhìn thế giới
    vec4 clip = vertexPos;
    vec4 view = gbufferProjectionInverse * clip;
    view /= view.w;
    vec3 rayWorld = normalize((gbufferModelViewInverse * vec4(view.xyz, 0.0)).xyz);

    float time = frameTimeCounter;

    // =========================================================================
    // 2. TỌA ĐỘ VÀ GÓC NHÌN CHUẨN XÁC POSTER GARGANTUA INTERSTELLAR
    // =========================================================================
    // Hắc lỗ ngự trị tại bầu trời phía Bắc, ngẩng 33 độ
    vec3 bhForward = normalize(vec3(0.0, 0.54, -0.84));
    vec3 bhRight = normalize(cross(bhForward, vec3(0.0, 1.0, 0.0))); 
    vec3 bhUp = cross(bhRight, bhForward);

    float u_screen = dot(rayWorld, bhRight);
    float v_screen = dot(rayWorld, bhUp);
    float w_screen = dot(rayWorld, bhForward);

    // Hệ số hướng về phía hắc lỗ để kiểm soát quầng sáng tỏa vào vũ trụ
    float facingBH = smoothstep(0.10, 0.45, w_screen);

    // Góc máy chuẩn xác Poster Interstellar (media_1792911348955_3d585515):
    // 1. Khoảng cách vừa vặn (Z = 21.0, Y = 0.92) & viewScale = 1.25
    // -> Hạ độ cao Y từ 1.40 xuống 0.92 giúp cân bằng hoàn hảo giữa vòm trên và vòm dưới
    // -> Nửa đĩa bên dưới có không gian mở rộng tự nhiên, uốn lượn đầy đủ và kết nối 100% vào hai cánh
    vec3 camPos = vec3(0.0, 0.92, 21.0);
    vec3 camDir = normalize(vec3(0.0, -0.044, -1.0));

    // Xoay trục góc nghiêng 36 độ theo phương chéo poster
    float theta = -36.0 * 0.01745329252;
    vec3 rawUp = vec3(-sin(theta), cos(theta), 0.0);
    vec3 camRight = normalize(cross(camDir, rawUp));
    vec3 camUp = normalize(cross(camRight, camDir));

    // viewScale = 1.25: Tỷ lệ hoàn hảo để 2 cánh thon dần và tiêu biến êm dịu trong khung hình
    float viewScale = 1.25;
    vec3 rayDir = normalize(camDir + (u_screen * camRight + v_screen * camUp) * viewScale);

    // Tham số vật lý Schwarzschild-Einstein chuẩn xác
    float M = 1.0;
    float rH = 2.0 * M; // Chân Trời Sự Kiện Schwarzschild (r = 2M)
    float rISCO = 2.60; // Bán kính quỹ đạo ổn định trong cùng
    float rOut = 15.5;  // Mép ngoài đĩa bồi tụ mở rộng kết nối mượt mà

    // =========================================================================
    // 3. TÍCH PHÂN QUỸ ĐẠO ÁNH SÁNG TƯƠNG ĐỐI TÍNH EINSTEIN (NULL GEODESICS)
    // Tăng số bước step lên 80 để vòm dưới hoàn thành trọn vẹn quỹ đạo uốn cong
    // Triệt tiêu hoàn toàn hiện tượng đứt cụt vòm dưới
    // =========================================================================
    vec3 p = camPos;
    vec3 v = rayDir;

    vec3 radAccum = vec3(0.0);
    vec3 transmittance = vec3(1.0);
    bool trappedInHorizon = false;
    float minApproach = 999.0;

    for (int step = 0; step < 80; step++) {
        float r = length(p);
        minApproach = min(minApproach, r);

        // Lọt vào Chân Trời Sự Kiện
        if (r <= rH * 1.015) {
            trappedInHorizon = true;
            break;
        }

        // Thoát ly an toàn ra khoảng cách xa
        if (r > 36.0 && dot(p, v) > 0.0) {
            break;
        }

        float dt = clamp(r * 0.075, 0.02, 0.80);

        // Gia tốc bẻ cong ánh sáng Einstein tuyệt đối chính xác
        vec3 h = cross(p, v);
        float h2 = dot(h, h);
        float r2 = r * r;
        float r5 = r2 * r2 * r;

        vec3 aGrav = - (3.0 * M * h2 / r5) * p;

        vec3 pNext = p + v * dt + 0.5 * aGrav * dt * dt;
        vec3 vNext = normalize(v + aGrav * dt);

        // =====================================================================
        // 4. BỨC XẠ ĐĨA BỒI TỤ XOÁY CHUYỂN ĐỘNG KEPLERIAN (LIVING VORTEX MOTION)
        // Chuyển động vi sai Keplerian: Lớp trong quay nhanh, lớp ngoài trôi chậm
        // =====================================================================
        bool hitPlane = false;

        // A. Cắt qua mặt phẳng đĩa xích đạo Y = 0 (bảo đảm 100% vòm trên & vòm dưới)
        if ((p.y * pNext.y) <= 0.0) {
            float denom = abs(pNext.y - p.y) + 1e-6;
            float tCross = clamp(-p.y / denom, 0.0, 1.0);
            vec3 pCross = p + tCross * (pNext - p);
            float rCross = length(pCross.xz);

            if (rCross >= rISCO && rCross <= rOut) {
                hitPlane = true;

                // Vận tốc góc vi sai Keplerian (Omega ~ 1 / r^(3/2)):
                // Sát chân trời quay cuồng mãnh liệt, ra xa trôi êm đềm uy nghi
                float phi = atan(pCross.z, pCross.x);
                float omega = 3.2 / (rCross * sqrt(rCross) + 0.25);
                float phiFlow = phi - time * omega;

                // Dòng xoắn ốc Logarithmic Keplerian (Spiral Streamers) theo phong cách Interstellar / iterationT:
                float spiral1 = sin(phiFlow * 2.0 - 2.6 * log(max(1.0, rCross)));
                float spiral2 = sin(phiFlow * 3.0 - 3.6 * log(max(1.0, rCross)) + 1.2);
                float armStreamers = pow(clamp(0.5 + 0.35 * spiral1 + 0.15 * spiral2, 0.0, 1.0), 1.6);

                // Các vành đai mật độ bụi khí
                float lanes = 1.0 + 0.22 * sin(rCross * 1.9 + 0.4 * spiral1) + 0.10 * sin(rCross * 4.0);

                // Tiêu biến dần dần mềm mại ở 2 mép: 2 cánh thuôn mượt và tan dần vào không gian
                float rho_r = smoothstep(rISCO, rISCO + 0.35, rCross)
                            * smoothstep(rOut, rOut - 4.5, rCross)
                            * pow(rISCO / rCross, 0.55);

                // Khói bụi fractal 3D cuộn trôi theo dòng xoáy vi sai + sôi động tự nhiên (IterationT Clumping Style)
                vec3 coord = vec3(rCross * 1.35 - time * 0.05, pCross.y * 5.5, phiFlow * 2.6);
                float rawSmoke = fbm3D(coord);
                float plasmaKnots = pow(rawSmoke, 2.4);
                float plasmaIntensity = 0.30 + 0.70 * plasmaKnots;

                float density = rho_r * lanes * (0.35 + 0.65 * armStreamers) * plasmaIntensity;

                // Vận tốc Keplerian & hiệu ứng Doppler
                float Omega = 1.0 / (rCross * sqrt(rCross));
                vec3 v_orb = vec3(pCross.z, 0.0, -pCross.x) / (rCross + 1e-5);
                float beta = clamp(rCross * Omega, 0.0, 0.75);
                float gamma = 1.0 / sqrt(max(0.05, 1.0 - beta * beta));
                float alpha_grav = sqrt(max(0.05, 1.0 - 2.0 * M / rCross));
                float cos_theta = dot(v_orb, -v);
                float g = clamp(alpha_grav / (gamma * (1.0 - beta * cos_theta)), 0.35, 2.5);

                float beaming = clamp(pow(g, 1.6), 0.50, 2.2);

                // Bảng màu Poster Interstellar:
                // Lõi & đĩa trong: Trắng lóa nóng chảy cực độ
                // Vùng giữa: Màu vàng hổ phách/cam mật đào rực rỡ
                // Vùng ngoài: Màu đồng/hổ phách lửa rực cháy
                float t_inner = clamp((4.4 - rCross) / 1.6, 0.0, 1.0);
                float t_mid   = clamp((9.2 - rCross) / 4.5, 0.0, 1.0);
                vec3 col_outer = vec3(1.10, 0.52, 0.18);
                vec3 col_mid   = vec3(1.65, 1.05, 0.55);
                vec3 col_inner = vec3(3.80, 3.50, 3.20);

                vec3 col_base = mix(col_outer, col_mid, t_mid);
                col_base = mix(col_base, col_inner, t_inner);

                // Độ dày quang học qua tấm mỏng
                float cos_inc = abs(v.y);
                float d_tau = (1.35 * density) / (cos_inc + 0.10);
                vec3 step_trans = exp(-d_tau * vec3(1.0, 1.05, 1.12));

                vec3 emission = col_base * beaming * (vec3(1.0) - step_trans) * 5.8;
                radAccum += emission * transmittance;
                transmittance *= step_trans;
            }
        }

        // B. Lấy mẫu thể tích khí bụi đối với các tia đi là là mặt đĩa
        vec3 pMid = 0.5 * (p + pNext);
        float rMid = length(pMid.xz);
        float h_disk = 0.08 + 0.035 * max(0.0, rMid - rISCO);

        if (!hitPlane && rMid >= rISCO && rMid <= rOut && abs(pMid.y) <= h_disk * 1.5) {
            float rho_y = exp(-7.0 * (pMid.y * pMid.y) / (h_disk * h_disk));
            float rho_r = smoothstep(rISCO, rISCO + 0.35, rMid)
                        * smoothstep(rOut, rOut - 4.5, rMid);

            float phi = atan(pMid.z, pMid.x);
            float omega = 3.2 / (rMid * sqrt(rMid) + 0.25);
            float phiFlow = phi - time * omega;

            float spiral1 = sin(phiFlow * 2.0 - 2.6 * log(max(1.0, rMid)));
            float spiral2 = sin(phiFlow * 3.0 - 3.6 * log(max(1.0, rMid)) + 1.2);
            float armStreamers = pow(clamp(0.5 + 0.35 * spiral1 + 0.15 * spiral2, 0.0, 1.0), 1.6);

            float lanes = 1.0 + 0.22 * sin(rMid * 1.9 + 0.4 * spiral1) + 0.10 * sin(rMid * 4.0);

            vec3 coord = vec3(rMid * 1.35 - time * 0.05, pMid.y * 5.5, phiFlow * 2.6);
            float rawSmoke = fbm3D(coord);
            float plasmaKnots = pow(rawSmoke, 2.4);
            float plasmaIntensity = 0.30 + 0.70 * plasmaKnots;

            float density = rho_y * rho_r * lanes * (0.35 + 0.65 * armStreamers) * plasmaIntensity;

            float t_inner = clamp((4.4 - rMid) / 1.6, 0.0, 1.0);
            vec3 col_base = mix(vec3(1.10, 0.52, 0.18), vec3(3.80, 3.50, 3.20), t_inner);

            float dl = length(pNext - p);
            float d_tau_vol = 1.5 * density * dl;
            vec3 step_trans_vol = exp(-d_tau_vol * vec3(1.0, 1.05, 1.12));

            vec3 emission_vol = col_base * (vec3(1.0) - step_trans_vol) * 4.9;
            radAccum += emission_vol * transmittance;
            transmittance *= step_trans_vol;
        }

        p = pNext;
        v = vNext;
    }

    // =========================================================================
    // 5. VÒNG PHOTON RING SẮC NÉT & HÀO QUANG KHÍ QUYỂN / QUẦNG SÁNG VŨ TRỤ (POSTER LOOK)
    // =========================================================================
    float distHorizon = max(0.0, minApproach - rH);
    float distCore = max(0.0, minApproach - 2.1);

    // Vòng photon ring thanh mảnh, chói lóa sắc nét ôm sát chân trời sự kiện
    vec3 glowPhoton = exp(-distHorizon * 5.5) * vec3(4.50, 4.00, 3.50) * 3.6;

    // Hào quang vàng kim ấm áp ôm lấy hắc lỗ và đĩa bồi tụ (Cinematic Golden Glow):
    // Tầng 1: Quầng sáng vàng kim rực rỡ ôm quanh thân đĩa bồi tụ
    // Tầng 2: Hào quang hổ phách dịu êm tỏa ra không gian lân cận, giữ nền trời sâu thẳm
    vec3 glowCorona = (exp(-distCore * 0.45) * vec3(1.35, 0.90, 0.45) * 0.85 + 
                       exp(-distCore * 0.20) * vec3(0.85, 0.45, 0.18) * 0.25);

    vec3 celestialBloom = (glowPhoton + glowCorona) * facingBH;

    // =========================================================================
    // 6. TỔNG HỢP & BẺ CONG KHÔNG-THỜI GIAN MƯỢT MÀ
    // =========================================================================
    vec3 finalColor;

    if (trappedInHorizon) {
        finalColor = radAccum;
    } else {
        // Hòa trộn thấu kính hấp dẫn mượt mà:
        float blendLens = clamp((18.0 - minApproach) / 12.0, 0.0, 1.0);
        vec3 vStar = normalize(mix(rayWorld, v, blendLens));

        vec3 bgStars = sampleStarsClean(vStar, time);
        finalColor = bgStars * transmittance + radAccum + celestialBloom;
    }

    // Quầng sáng huyền ảo đáy Cõi Thai Nghén
    float horizonGlow = smoothstep(0.0, 0.10, rayWorld.y) * smoothstep(0.35, 0.0, rayWorld.y);
    finalColor += vec3(0.04, 0.015, 0.08) * horizonGlow;

    // Áp dụng Tone-Mapping ACES Filmic
    vec3 mapped = aces_tonemap(finalColor);

    gl_FragData[0] = vec4(mapped, 1.0);
}
