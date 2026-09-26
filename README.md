# Kayji-HotPotatoAdvanced

Plugin Minecraft (Spigot) **mini-game Khoai Tây Nóng (Hot Potato)**: quả bom truyền tay với đếm ngược, vùng bo vuông thu nhỏ, hệ thống điểm, bảng xếp hạng, nhiều chế độ chơi và GUI cấu hình ngay trong game.

> **Tác giả:** Kayji_Tizi · **Phiên bản:** 2.0 · **API:** 1.20 · **Java:** 17 · **Softdepend:** Vault

## Tính năng

- **Trò chơi khoai tây nóng**: truyền bom với thời gian nổ cài đặt (`explosion_time`), hiệu ứng sấm sét/âm thanh/particle đường đi, có thể tắt kill khi nổ (`hotpotatotogglekill`).
- **Vùng bo vuông** — đặt tâm + bán kính (`/hotpotatozone set [radius]`), tùy chọn **thu nhỏ dần** (`zone_shrink_enabled`, `zone_shrink_rate`), chỉnh kích thước ngay trong game (`expand`/`shrink` 5 block).
- **Hệ thống điểm** (`point_system`): điểm theo thời gian sống (`points_per_survival`), xem điểm cá nhân, **bảng xếp hạng top server** + **GUI leaderboard**.
- **4 chế độ chơi**: `normal`, `freeze`, `tag`, `lms` — chọn mặc định trong `modes.default`.
- **Bảo vệ truyền bom** (`pass_protection`): thời gian cầm tối thiểu, không được truyền trong giây cuối, cooldown nhận bom.
- **Thưởng cuối trận** (`rewards`): pool thưởng quản lý bằng GUI, broadcast kết quả, người chơi mở **kho thưởng cá nhân** (`/hotpotatoreward`).
- **GUI cấu hình in-game** (`/hotpotatoconfig`) — sửa cài đặt không cần đụng file.
- **Thống kê + webhook Discord** (`stats.discord_webhook`), âm thanh đếm ngược, pháo hiệu nổ, vùng bo phát sáng.
- Tương thích **Vault** (softdepend) cho phần thưởng tiền tệ.

## Bảng lệnh

Gõ trong game với dấu `/`:

| Lệnh | Quyền | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `/hotpotato` | `hotpotato.start` | op | Bắt đầu trận đấu |
| `/hotpotatoreset` | `hotpotato.reset` | op | Dừng trận và reset trạng thái |
| `/hotpotatopoints` | `hotpotato.points` | **mọi người** | Xem điểm của bạn |
| `/hotpotatotop` | `hotpotato.top` | **mọi người** | Xem top nhà vô địch |
| `/hotpotatoleader` | `hotpotato.leader` | **mọi người** | Mở GUI bảng xếp hạng |
| `/hotpotatoreward` | `hotpotato.reward` | **mọi người** | Mở kho thưởng cá nhân |
| `/hotpotatozone <set\|clear> [radius]` | `hotpotato.zone` | op | Đặt/xóa tâm + bán kính vùng bo |
| `/hotpotatoexpand` | `hotpotato.resize` | op | Mở rộng vùng bo 5 block |
| `/hotpotatoshrink` | `hotpotato.resize` | op | Thu hẹp vùng bo 5 block |
| `/hotpotatotimeadd` | `hotpotato.time` | op | +5 giây thời gian nổ |
| `/hotpotatotimereduce` | `hotpotato.time` | op | −5 giây thời gian nổ |
| `/hotpotatotogglekill` | `hotpotato.togglekill` | op | Bật/tắt giết người cầm bom khi nổ |
| `/hotpotataconfig` | `hotpotato.config` | op | Mở GUI cấu hình |
| `/hotpotatorewardsetup` | `hotpotato.reward.setup` | op | Mở GUI quản lý pool thưởng |

## Cấu hình (tóm tắt)

```yaml
explosion_time: 20              # giây
explosion_power: 2.0
point_system: true
use_action_bar: true
randomize_timer: false
auto_restart: false

zone_radius: 15
min_zone_radius: 5
zone_shrink_enabled: false
zone_shrink_rate: 1

leaderboard: { gui_size: 9, broadcast_top: 3 }

modes:
  default: "normal"
  available: ["normal", "freeze", "tag", "lms"]

stats:
  discord_webhook: ""           # ⚠️ điền webhook nếu muốn, KHÔNG commit token thật
  broadcast_summary: true

pass_protection:
  enabled: true
  min_hold_time: 3              # giây cầm tối thiểu trước khi truyền
  no_pass_last_seconds: 3       # cấm truyền trong 3 giây cuối
  receive_cooldown: 2

rewards: { enabled: true, announce: true }
```

## Cài đặt

```bash
mvn clean package
```

1. Copy `target/Kayji-HotPotato-v2.0.jar` vào thư mục `plugins/`.
2. (Tùy chọn) Cài **Vault** nếu muốn thưởng tiền.
3. Chạy server một lần để tạo `config.yml`, rồi restart.

> Maven đóng gói `gson` và `VaultAPI` (loại trừ `spigot-api`).

## Cấu trúc dự án

```
├── pom.xml                                          Maven (Java 17, spigot-api 1.20.1)
└── src/main
    ├── java/com/kayjifamily/hotpotatoadvanced
    │   ├── HotPotatoFull.java                       Lớp chính
    │   ├── commands/HotPotatoCommands.java          14 lệnh
    │   ├── game/GameManager.java                    Vòng đời trận đấu
    │   ├── zone/ZoneManager.java                    Vùng bo vuông
    │   ├── points/PointManager.java                 Hệ thống điểm
    │   ├── leaderboard/LeaderboardManager.java      Bảng xếp hạng + GUI
    │   ├── reward/RewardManager.java                Pool thưởng
    │   ├── mode/ModeManager.java                    4 chế độ chơi
    │   ├── gui/ConfigGUI.java                       GUI cấu hình
    │   ├── stats/StatsManager.java                  Thống kê + webhook
    │   └── util/ / config/ / data/                  Hiệu ứng, cấu hình, dữ liệu
    └── resources
        ├── plugin.yml                               14 lệnh + quyền
        └── config.yml                               Thời gian nổ, zone, mode, thưởng
```

## Giấy phép

[GNU General Public License v3.0](LICENSE)
