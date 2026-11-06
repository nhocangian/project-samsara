# Project Samsara – Tổng hợp trạng thái (bàn giao cho phiên tinh chỉnh)

Cập nhật: 07/11/2026. Đọc kèm `CLAUDE.md` (cách build/chạy), `scripts/master_production_document_project_samsara_revision.md` (tài liệu kịch bản: triết lý, bảng stage, 9 video, thoại `<System>`, outro, Chu Kỳ #2; ~4000 dòng, tìm theo tiêu đề rồi đọc phần cần).
Một số quyết định dưới đây **thay đổi so với tài liệu gốc**; khi mâu thuẫn, theo file này.

## Cách chạy nhanh
- Nhấp đúp `D:\c\start_samsara.bat`: tự build rồi mở dev client (Iris + Sodium). Log ở `mod\run\samsara_client.log`. Cửa sổ đen phải để mở khi chơi.
- Sửa shader: `shader/Samsara-Cosmic-Shaders` (junction vào `mod/run/shaderpacks`), tải lại shader trong game là thấy. Có `README.md` mô tả shader và bản đóng gói `shader/Samsara-Cosmic-Shaders.zip` (chụp tại một thời điểm; nguồn thật là thư mục).
- Mọi tính năng có lệnh `/samsara test <...>`; gõ `/samsara test help` để xem danh sách.

## Tình trạng các giai đoạn
| Giai đoạn | Trạng thái |
|---|---|
| 1. Vòng chơi lõi | Xong, đã thử trong game |
| 2. Cốt truyện Overworld + Ký Ức Của Nam | Xong; nhà và cánh đồng đã thử, các phần khác chưa ai thử |
| 3a. Quả Trứng, cinematic, đoạn thoại `<System>`, màn kết | Xong, đã thử |
| 3b. Tuyến hành hương The Womb (4 chương, thuyền, Wanderer, âm thanh) | Xong; phần Wanderer bám đuôi và âm thanh **chưa thử** |
| 4. Đánh bóng | Nhóm 1–4 xong và đã duyệt; còn nhóm 5 cân chỉnh tổng (xem cuối file) |

Đã **bỏ hẳn** theo quyết định của người dùng: Elysian Highlands, nghi thức tọa độ gương / Stasis Cube, băng ký ức ở khổng thần, "ký ức của riêng bạn" (đồ đánh mất, mảnh đất chụp từ Overworld). World "Chu Kỳ #N" đã làm ở Giai đoạn 4.

## Mạch chơi hiện tại (từ đầu tới cuối)
1. **Overworld**: mỗi giấc ngủ qua đêm tăng `sleep_count` của cả thế giới.
   - Stage 1 (lần ngủ thứ 1–2): cừu đứng im nhìn về Bia Mộ (Spawn Cairn), nhạc nền bị giật.
   - Stage 2 (3–4): nước Overworld màu xám bạc, cây chỉ lớn khi được ngắm qua máy quay bật IR, bóng Nam_1998 vây quanh lúc đêm (chỉ thấy qua máy quay).
   - Stage 3 (5–6): cát/sỏi bay ngược lên trời, la bàn quay loạn.
   - Stage 4 (7+): mọi block tự nhiên trên mặt đất bị hút lên.
   - Không ngủ thì bầy Phantom đông dần theo số đêm thức; ngủ dậy là bầy tan.
   - Máy quay do `<System>` cài vào ô số 3 khi thức trắng tới đêm thứ 3 hoặc khi thế giới lên Stage 2.
2. **Giấc ngủ thứ 7**: hiệu ứng không gian kéo dãn (FOV giãn, vệt sao), rồi tỉnh dậy ở **Ký Ức Của Nam** (`samsara:nam_memory`).
   - Cánh đồng hướng dương phẳng, một con đường mòn duy nhất, nhà Nam "Tháng 5/2018" cùng cối xay gió.
   - Shader `world2` bẻ cong thế giới kiểu Inception (chỉ thấy khi bật shader).
   - Hoa hướng dương là block riêng `memory_sunflower`. Lúc sinh, hoa quay về nơi người chơi xuất hiện; sau đó `system/memory/SunflowerGazeSystem` cứ 10 tick xoay các bông trong bán kính 48 về phía người chơi (mỗi bông 40% cơ hội mỗi lượt nên quay lệch nhịp). Lệnh `/samsara test sunflower_gaze`.
   - Dưới hầm nhà: rương có "Trang Xé Cuối Cùng" và vết rách sang The Womb.
3. **The Womb**: tới **Bến Đò** (gốc tuyến). Tuyến hành hương dài 400 block về phía bắc:
   - I. Bến Đò: bến gỗ mục, giường đỏ chìm, mảnh đất Overworld lơ lửng, thuyền chờ. Dòng chảy đẩy thuyền; qua máy quay thấy thuyền bóng ma "chính bạn" chèo song song.
   - II. Những Cánh Cửa: 16 cửa save file có tên (2021 lùi về 2009), 12 cửa rừng xa, tinh thể tím lơ lửng.
   - III. Ba khổng thần: Steve 2009 quỳ; hố nổ 2013 với mặt Creeper dưới đáy và mảnh vụn đóng băng; kẻ đốt nhà nâng căn nhà cháy.
   - IV. Tháp ngược: cầu thang xoắn nổi lên độ cao ~100, cánh cửa cuối "New World – bây giờ – bạn", hồ gương lơ lửng có Quả Trứng, tháp treo ngược từ trời.
   - Suốt tuyến, The Wanderer bám đuôi: chỉ thấy qua máy quay; tiến lại khi không bị nhìn, đứng sững khi bị nhìn qua ống kính; chạm vào thì bị ném lùi về chặng trước. Mỗi chương có tiêu đề và âm nền riêng; nhịp tim trứng to dần.
4. **Đoạn kết** (bước vào trong 6 block quanh trứng):
   - Cinematic 15 giây: toàn cảnh xa, rồi sát vai, rồi bóng phản chiếu trồi lên, rồi về mắt nhân vật.
   - Đoạn thoại tự động qua chat theo tài liệu mục IV. Bóng phản chiếu nhảy kích thước ×2, ×4, ×12, ×45; câu cuối Gargantua là con mắt nó. Tên nhân vật: Nam_1998, rồi bị nhiễu, rồi username thật.
   - Kết: nhắm mắt, trứng nở, màn NO SIGNAL, câu kết, lưu & thoát về menu.

## Bản đồ code (`mod/src/main/java/com/samsara/`)
- `system/cycle/`
  - `EggCycleState`: **trạng thái duy nhất** của thế giới: số lần ngủ, stage, đã nhận máy quay, đã vào Ký Ức, nhà Nam, gốc tuyến, vị trí trứng, đã xem kết.
  - Các hệ thống chạy theo stage: `PhantomTrapSystem`, `CamcorderGiftSystem`, `StageAnomalySystem` (cừu, cây trồng IR), `CollapseAnomalySystem` (block bay lên), `NamGhostSystem`, `WombTransitionSystem` (hiệu ứng giấc ngủ thứ 7).
- `system/celestial/SolarFractureManager`: đếm giấc ngủ khi thức dậy. **Dùng `timeOfDay % 24000 < 100`, KHÔNG dùng `isDay()`**, vì lúc thức dậy `isDay()` vẫn còn giá trị ban đêm. Đồng bộ stage xuống client.
- `system/memory/NamMemoryManager` + `worldgen/memory/`: chiều Ký Ức Của Nam (`NamMemoryLayout`, `SunflowerFieldFeature`, `NamHouseBuilder`).
- `system/womb/`
  - `WombRoute`: hình học tuyến (các chặng, `progress`, `pointAlong`, `tangentNear`).
  - `WombRouteManager`: dựng tuyến (hành lang, phao đèn, gọi các chương); cấm phế tích ngẫu nhiên trong 55 block quanh tuyến và tự dọn lại hành lang.
  - `WombTravel`: vào/ra The Womb.
  - `WombCurrentSystem`: đồng bộ gốc tuyến xuống client, giữ thuyền ở bến.
  - `WanderChaseSystem`, `WombPacingSystem`.
  - `chapters/`: `DockChapter`, `SaveDoorChapter`, `TitanChapter` + `TitanBody` (dựng thân Steve bằng block), `SpireChapter`, `FloatingCrystals`.
- `system/ending/`: `EndingServer` (kích hoạt kết), `EggAltarManager` (hồ trứng + con dấu 7 tia).
- `entity/PrimordialEggEntity` + `entity/client/PrimordialEggRenderer`: trứng voxel 6 block, nhịp 60/phút, 7 mảnh vỏ.
- `client/ending/`: `EndingSequenceClient` (dòng thời gian, camera, rung), `EndingDialogue` (kịch bản), `MirrorSelfRenderer` (bóng phản chiếu), `NoSignalScreen`.
  - Bóng phản chiếu (hình dạng của `<System>`): 3 lớp vẽ: thân `RenderLayer.getEndPortal()` (trời sao), skin mờ `getEntityTranslucent`, viền `getEnergySwirl` (texture creeper_armor) phồng 6%. Đầu bắt chước hướng nhìn người chơi qua mặt gương (`getMirrorLook`, ghi lịch sử 32 tick), trễ `MIRROR_LAG` = 10 tick từ câu `<System>` đầu tiên, thôi bắt chước từ ×12. Nhảy cỡ = `GLITCH_TICKS` = 10 tick chớp giữa cỡ cũ/mới + rung ngang + tách màu + mất hình.
  - Từ ×12 tới ×45, `getReflectionPos` trượt dần để mắt (cao `EYE_HEIGHT` 1.75 × cỡ) nằm trên đường từ mắt người chơi theo `GARGANTUA_DIR` = (0, 0.54, -0.84) (phải khớp `bhForward` của `shader/.../world1/gbuffers_skybasic.fsh`).
  - **Tín hiệu cho shader:** khi mắt lộ ra ở The Womb, client đặt `world.setRainGradient` 0→1 trong ~2 s (Iris `rainStrength`; The Womb không có mưa). Về 0 khi `reset()`. Yêu cầu phía shader ghi ở `shader/GHI_CHU_CHO_AGENT_SHADER.md`.
  - Lệnh `/samsara test mirror <1-45|off>`: xem trước bóng trước mặt (pha `PREVIEW`, không khoá di chuyển, trễ nửa nhịp sau 3 s).
- `client/`: `StageAnomalyClient` (nhạc giật, nước xám), `WombTransitionClient`, `WombRouteClient` (dòng chảy, thuyền bóng ma, gửi trạng thái ngắm máy quay), `WombAmbienceClient`.
- Mixin mới (trong `samsara.mixins.json`):
  - Server: `CropBlockMixin`.
  - Client: `MusicTrackerAccessor`, `BiomeWaterColorMixin` (Sodium bỏ qua FluidRenderHandler nên phải đổi màu nước ở Biome), `GameRendererFovMixin`, `CompassSpinMixin`, `ChatScreenAccessor`, `CameraEndingMixin`, `KeyboardInputMixin`.
- Texture sinh bằng script trong `mod/tools/`: `generate_womb_stretch.py`, `generate_primordial_egg.py`, cùng các script mặt trời. Sửa script rồi chạy lại, không sửa PNG bằng tay.

## Lệnh test quan trọng
- Chu kỳ: `cycle`, `sleep_count <n>`, `phantom_wave [đêm]`, `camcorder_gift`, `anomaly sheep|music|lift [3|4]|reverse [phantom|cave]`, `nam_ghosts`.
- Ký Ức Của Nam: `/samsara nam`, `nam_house`, `nam_field [r]` (trồng lại hoa cho chunk cũ), `womb_transition`.
- The Womb: `/samsara womb`, `womb_route` (liệt kê chặng), `womb_route rebuild|dock|titans|spire|here|tp <0-4>`, `wanderer_chase [start|stop]`, `chapter_title <1-4>`.
- Kết: `egg`, `ending` (cuối cùng sẽ lưu và thoát về menu), `mirror <1-45|off>` (xem trước bóng phản chiếu).

## Lưu ý / nợ kỹ thuật
- **Dữ liệu Elysian còn giữ** để world cũ mở được: `data/samsara/dimension/elysian_highlands.json`, `dimension_type/elysian_highlands.json`, `worldgen/biome/elysian_meadows.json`. Không còn code nào dẫn tới đó; xoá được khi chuyển sang world mới. Code cũ nằm ở `_trash/elysian_mirror_removed/`.
- `LucidSlumberManager` đã bị xoá; `WombArchitectureGenerator.generateWombSanctuary` và block `primordial_egg` cũ giờ không còn dùng (block vẫn giữ để không hỏng save).
- World đã sinh trước khi có tính năng mới cần dựng lại bằng lệnh (`womb_route rebuild`, `nam_field`...).
- Chữ trong game nằm trong `assets/samsara/lang/vi_vn.json` (xem mục Tiếng Việt hoá bên dưới).
- Âm thanh toàn bộ là âm vanilla đổi pitch; chưa có file `.ogg` riêng. **Âm tua ngược thật**: trong `sounds.json`, tên `samsara:reversed/<ns>/<đường_dẫn>` = file `<ns>:sounds/<đường_dẫn>.ogg` phát ngược, đảo PCM trong bộ nhớ lúc nạp (`client/sound/ReversedSounds`, `SoundLoaderReverseMixin`, `SoundManagerReverseMixin`; không dùng với `stream: true`). Đang dùng cho `entity.phantom_reversed`, `ambient.cave_reversed` và `ambient.reversed_familiar` (Stage 3+, `system/cycle/ReversedEchoSystem`: đứng yên ≥3 s ở Overworld, ~1/25 mỗi giây, nghỉ 150 s; chỉ người đó nghe, phát sau lưng 3–5 block). Lệnh `/samsara test anomaly reverse [phantom|cave]`.
- Chưa kiểm tra đầy đủ: nước xám khi bật shader (phụ thuộc shader có dùng màu đỉnh hay không), độ trơn của cầu thang xoắn ở các góc, thời lượng đoạn thoại kết (khoảng 3–4 phút).
- Dự án là git repo, remote `origin` = github.com/nhocangian/project-samsara (nhánh `main`).

## Giai đoạn 4 – đã chốt phạm vi (06/11/2026)
Người dùng giữ 5 nhóm, **bỏ nhóm âm thanh riêng** (.ogg tự làm). Thứ tự đề xuất:
1. **Kinh dị tầng giao diện**: tin nhắn "Nam_1998 joined the game" trong world chơi đơn, advancement giả, dòng F3 lạ, tên world tự đổi sau khi thoát, menu tạm dừng bị nhiễu ở Stage 4.
2. **Model mới**: The Wanderer làm lại (cổ lệch, tay dài quá gối, giật đầu), máy quay 3D khi cầm (nắp băng, đèn REC), Phantom qua ống kính thành thân người có màng cánh.
3. **Tiếng Việt hoá**: chuyển `Text.literal` sang lang key + `vi_vn.json`, sửa các dòng tiếng Anh còn sót.
4. **World "Chu Kỳ #2"**: sau màn kết, tạo world mới có nhà Nam ở điểm spawn. Người dùng đã đồng ý; **chỉ tạo mới, không đụng world cũ**.
5. **Cân chỉnh tổng**: tốc độ dòng chảy, nhịp The Wanderer, thời lượng chương và đoạn thoại kết, độ hiếm dị tượng. Cần người dùng chơi thử trọn một lượt rồi báo lại.

Trạng thái (cuối phiên 06/11/2026):

**Đã xong (người dùng đã duyệt trong game):**
- **Nhóm 1 – Kinh dị tầng giao diện** (`system/ui/UiHorrorSystem`, `client/ui/UiHorrorClient`, `FakeAdvancementToast`, `DebugHudMixin`, `WindowTitleMixin`, `GhostPlayerListMixin`, `TitleScreenAccessor`, `LevelPropertiesAccessor`):
  - Nam_1998 "joined/left the game" (từ Stage 2, ban đêm); từ Stage 3 mỗi lần vào gõ đúng một câu "em đang ở đâu" (`UiHorrorSystem.GHOST_LINE`, theo kịch bản Video 3; các câu khác đã bỏ). Có mặt trong bảng Tab và gợi ý tên lúc đó (cột sóng chập chờn).
  - Thành tựu giả mỗi mốc một lần: Mắt Thứ Ba, 4 stage, Có Ai Đó Đứng Đó, Về Nhà, Đừng Rời Mắt.
  - Dòng F3 lạ (Stage 1+ / 2+ / 3+); tiêu đề cửa sổ "Minecraft 1.20.1-SAMSARA-REL" từ Stage 1; nút "Rời giường" chớp "ĐỪNG NGỦ" từ Stage 1.
  - Tên thế giới đổi khi thoát (Stage 3 "<tên> - Nam_1998", Stage 4 "Nam_1998", sau kết "đừng mở lại"; tên gốc lưu trong `EggCycleState`).
  - Menu tạm dừng nhiễu + nút Lưu & Thoát chớp "Tạo thế giới mới" (Stage 4). Sau đoạn kết: menu chính đổi câu chào "Quả trứng tiếp theo đã sẵn sàng." và nút Chơi đơn chớp (cờ ở `mod/run/config/samsara_client_state.txt`).
  - Lệnh `/samsara test ui <join|leave|say|advancement|rename|reset|ending_menu_on|ending_menu_off>`.
- **Nhóm 2 – The Wanderer** làm lại theo concept art (`TheWandererModel`, `TheWandererRenderer`, `mod/tools/generate_wanderer.py`; lệnh `/samsara test wanderer`):
  - ~90 vạt vải phẳng (mép rách trong suốt) xếp 7 vòng chồng lớp; lơ lửng (không chân, không bóng), tà áo bị gió nâng lượn sóng; mắt 2 px phát sáng.
  - **Quầng sáng mắt bằng shader** cho mọi thứ dùng lớp `RenderLayer.getEyes`: `gbuffers_spidereyes` ghi vào `colortex4` -> `lib/eye_glow.glsl` trong `final.fsh` (mọi thư mục world; `world2` có bloom riêng dùng colortex1/2).
- Hiệu ứng rè camera khi bóng ma lại gần: nhiễu băng VHS (`CamcorderOverlay.drawVhsInterference`) thay cho vạch trắng.
- Sửa lỗi: khi cầm máy quay, "nhìn đêm" (vanilla + Iris) chỉ theo phím V, bỏ qua thuốc Nhìn Đêm (`NightVisionCamcorderMixin`).

**Đã xong và đã duyệt (thêm):**
- **Nhóm 2 – Phantom qua ống kính** ("Tàn Hồn Phantom"): `SoulPhantomRenderer` thay renderer Phantom vanilla (đăng ký trong `SamsaraClientMod`); mắt thường vẫn vẽ Phantom vanilla, qua máy quay vẽ `SoulPhantomModel` (thân người gầy trơ sườn, tay dang làm xương cánh, màng da rách, chân kéo lê + màng đuôi, mắt phát sáng; vỗ cánh cùng nhịp vanilla). Texture: `mod/tools/generate_soul_phantom.py`. Lệnh `/samsara test soul_phantom` (Phantom không AI đứng trước mặt).

**Đã xong và đã duyệt (thêm, 06/11/2026):**
- **Nhóm 2 – Máy quay 3D khi cầm** (`client/camcorder/CamcorderItemModel`, `CamcorderItemRenderer`, đăng ký `BuiltinItemRendererRegistry` trong `SamsaraClientMod`; model JSON `fractured_slate.json` giờ là `parent: builtin/entity`, chỉ giữ `display`). Texture `textures/item/camcorder.png` sinh bởi `mod/tools/generate_camcorder.py` (xem trước: `mod/tools/preview_camcorder.png`).
  - Bộ phận: thân, nòng kính + loa che, micro, đèn REC, đèn IR, nắp băng (có cuộn băng bên dưới), kính ngắm + cao su mắt, pin, dây đeo, màn LCD.
  - Hoạt ảnh: LCD lật mở (bật lò xo, tiếng click) mỗi lần cầm lên ở góc thứ nhất, chờ ~0.7 s cho tay đưa item lên rồi lật trong ~0.8 s (`LCD_DELAY_TICKS`, `LCD_OPEN_TICKS`); màn sáng xám xanh, xanh lục khi bật IR, tắt khi hết pin, chập chờn khi bóng ma gần. Đèn REC nháy 1 lần/giây khi còn pin. Đèn IR sáng tím khi nhìn đêm. Nắp băng bật lên rồi đóng khi thay pin (R). Góc thứ ba: LCD luôn mở; túi đồ/khung/đất: gập.
  - Trạng thái hoạt ảnh nằm trong `CamcorderClientManager` (`getLcdOpen`, `getLidOpen`).
  - Mặt màn LCD vẽ bằng lớp có cull (`getEntityCutout`) và lùi 0.2 px khỏi vỏ; dùng lớp no-cull sẽ bị sọc z-fighting.
  - **Động tác đưa lên mắt** (`HeldItemRendererMixin`): neo ở cao su mắt kính ngắm, đi đường cong (nhấc lên -> vào giữa -> áp mắt), nghiêng nhẹ giữa chừng rồi khử góc cầm ở hông; ~0.35 s (`aimProgress` +0.14/tick). Hằng số `DISPLAY_*` phải khớp `firstperson_righthand` trong `fractured_slate.json` (tay trái khai báo giống tay phải vì Minecraft tự lật trục X).
  - Bỏ hoạt ảnh "nhấn nút" của vanilla: `FracturedSlateItem.use` trả `consume` (không vung tay) và mixin chặn `HeldItemRenderer.resetEquipProgress` khi tay đang cầm máy quay.
  - Lệnh `/samsara test camcorder_model` (cầm máy, pin về 50%, thêm 2 pin).

**Nhóm 3 – Tiếng Việt hoá** (xong, người dùng đã duyệt trong game 06/11/2026):
- `vi_vn.json` là bản gốc duy nhất (~216 key). `en_us.json` **không có trong src**: `processResources` trong `build.gradle` chép `vi_vn.json` thành `en_us.json`, nên để ngôn ngữ nào cũng thấy tiếng Việt (người dùng chọn vậy; người dùng chơi bằng en_us). File en_us cũ cất ở `_trash/lang_before_vi/`.
- Key đặt theo lớp: `samsara.<tên_lớp>.<vài_chữ_đầu>` (vd. `samsara.phantom_trap_system.ngu_di`), riêng `samsara.ending.*`, `samsara.advancement.*`, `samsara.ui_horror.ghost.*`, `samsara.anvil_mirror.*`, `samsara.palimpsest_journal.nam_page_N / last_page_N`.
- Thành tựu thật (`data/samsara/advancements/*.json`) dùng `{"translate": "advancements.samsara.<tên>.title|description"}`.
- Server gửi `Text.translatable` (client dịch). Chỗ cần `String` phía client (HUD máy quay `CamcorderOverlay`, kịch bản `EndingDialogue` vì đoạn chat gõ từng chữ) dùng `I18n.translate`. Sách: trang là JSON `{"translate": key}`; tên/tác giả sách vẫn là chuỗi thường (Minecraft không dịch được).
- Tiêu đề thành tựu giả và lời Nam_1998 truyền dưới dạng key; truyền chuỗi thường vẫn hiện đúng (key lạ hiện nguyên văn), nên lệnh test cũ không cần sửa.
- Có tham số dùng `%s`; ký tự phần trăm thật viết `%%` (chuỗi dùng `I18n.translate` cũng phải tuân theo).
- Đã dịch sang tiếng Việt các dòng tiếng Anh còn sót (Chiếc Nôi, Metempsychosis, Ouroboros, phế tích, tên item/entity/phím). Cố ý giữ: chữ kỹ thuật kiểu máy quay (PLAY, VCR REWIND, NO SIGNAL, OBS ■ REC STOPPED, GARGANTUA BEACON), dòng F3, tên riêng (Nam_1998, New World, tên save trên các cánh cửa, tên khúc tuyến trong `WombRoute`). Lệnh debug (`SamsaraTestCommands`, lệnh phế tích/cự thạch) vẫn viết thẳng trong code.

**Nhóm 4 – World "Chu Kỳ #N"** (xong, người dùng đã thử và duyệt 06/11/2026):
- `NoSignalScreen.exitToTitle` (chơi đơn): `NewCycleClient.capture` chép LevelInfo (chế độ, hardcore, độ khó, cheat) + DataConfiguration của world đang chơi và đọc `EggCycleState.cycle`; lưu & thoát xong thì `NewCycleClient.start` gọi `IntegratedServerLoader.createAndStart` tạo world "Chu Kỳ #N+1" (thư mục `Chu Ky N`, seed ngẫu nhiên, preset mặc định; dimension của mod tự gộp từ datapack). Lỗi thì về menu chính như cũ. World cũ chỉ được lưu, không bị sửa.
- Số chu kỳ truyền sang server tích hợp qua `NewCycleSystem.pendingCycle` (cùng JVM). `SERVER_STARTED`: ghi `EggCycleState.cycle` (NBT `Cycle`, mặc định 1) và dựng nhà Nam 12 block về phía bắc điểm spawn, cửa quay về spawn (`NamHouseBuilder.buildForNewCycle`: đắp nền đất, dọn cao hơn, **không có vết rách**, rương hầm để trống). Rương Bia Mộ ở chu kỳ ≥2 chỉ có nhật ký, không có hoa khô (`SpawnCairnGenerator` đọc `EggCycleState.cycle` lúc JOIN; `SERVER_STARTED` đã ghi số chu kỳ trước đó). Bia Mộ vẫn ở spawn + (3, 3).
- Lần đầu vào world chu kỳ mới (sau 3 giây): tiêu đề "Chu Kỳ #N", phụ đề "quả trứng tiếp theo", một dòng <System>. Mọi cơ chế chu kỳ (ngủ, stage, máy quay...) bắt đầu lại từ đầu trong world mới.
- Lệnh `/samsara test new_cycle house` (dựng nhà cách bạn 12 block về phía bắc) / `intro` / `cairn` (Bia Mộ bản chu kỳ mới, 3 block phía bắc). `/samsara test ending` giờ cũng tạo world mới khi kết thúc.

**Kịch bản:** người dùng làm kịch bản ở một project riêng chỉ chứa `scripts/`. File `scripts/hien_trang_trong_mod.md` mô tả (theo góc nhìn câu chuyện) những gì mod đã dựng, chỗ khác tài liệu gốc và hệ thống phụ. **Phiên mod nào thay đổi nội dung người chơi thấy thì cập nhật file đó.** Khi người dùng đưa kịch bản mới, đối chiếu với file đó để biết phải sửa gì trong game.

**Đã dựng 07/11/2026 (4 việc từ đề xuất kịch bản, chưa thử trong game):**
- Trang nhật ký [ĐÊM ĐẦU TIÊN] viết lại (mắt thấy bình thường, ảnh chụp có chấm đen).
- **Ảnh F2 thấy mặt trời thật** (`client/ScreenshotSunClient`, `KeyboardScreenshotMixin`, `MinecraftClientScreenshotMixin`): bấm F2 ở Stage 1+ (không đang ngắm máy quay) thì chép khung đang hiện vào một framebuffer phụ, khung kế tiếp vẽ mặt trời hố đen (`WorldRendererMixin` đọc `isCapturing()`), chụp khung đó trước khi lên màn hình rồi đưa khung cũ lên thay. Lệnh `/samsara test screenshot_sun`.
- Chu Kỳ ≥2: lần đầu Nam_1998 vào game gõ `UiHorrorSystem.ARCHIVE_LINES` (3 dòng Video 3) rồi rời đi; cờ thế giới `markUiFlag(NAM_UUID, "archive_lines")`. Lệnh `/samsara test ui archive`; `ui reset` xoá cả cờ này.
- Màn NO SIGNAL ghi `2021-03-14 03-29-51.mkv   00:03:42:09`.

**Việc còn lại cho phiên sau:**
1. **Cân chỉnh tổng** (nhóm 5): **TẠM HOÃN** (07/11/2026, người dùng: mod chưa xong nên chưa cân chỉnh; đừng đề xuất lại cho tới khi người dùng nói mod đã đủ nội dung). Khi làm: cần người dùng chơi trọn một lượt rồi báo lại (tốc độ dòng chảy, nhịp The Wanderer, thời lượng chương / đoạn thoại kết, độ hiếm dị tượng, độ mạnh `EYE_GLOW_STRENGTH`).
