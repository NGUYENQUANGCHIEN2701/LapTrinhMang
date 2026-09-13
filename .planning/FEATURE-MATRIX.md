# Feature matrix từ hai tài liệu đặc tả

Mục tiêu của bảng này là ngăn việc triển khai toàn bộ tính năng cùng lúc. MVP phục vụ bản demo/chấm điểm; Backlog chỉ làm sau khi realtime, campaign và persistence ổn định.

| Nhóm | Tính năng | Ưu tiên | Phase |
|---|---|---:|---:|
| Navigation | Main menu, game submenu, back/exit | MVP | 5 |
| Campaign | Campaign selector, unlock tuần tự | MVP | 4 |
| Mission | Briefing trước trận | MVP | 4 |
| Mission | Kill all enemies | MVP | 4 |
| Mission | Survive timer | MVP | 4 |
| Mission | Protect target | MVP | 4 |
| Result | Victory/debriefing, coins, stars, time, shots, damage | MVP | 4 |
| Save | Autosave và 3 normal save slots | MVP | 5 |
| Save | 20 slot thường và 20 bonus slot | Backlog | 5+ |
| Settings | Volume, language, game scale, UI size, FPS, reset | MVP | 5 |
| Gameplay | Movement, shooting, HP, collision | MVP | 3 |
| AI | Patrol, chase, rotate, shoot | MVP | 3 |
| AI | Dijkstra/pathfinding nâng cao | Backlog | 7 |
| Mode | Practice map procedural 20x20 | Backlog | 6 |
| Mode | Online Levels độc lập | MVP | 6 |
| Shop | Coins, basic ammo, repair kit | MVP | 6 |
| Shop | Plasma, artillery, rocket, mine, dynamite, air support | Backlog | 7 |
| Shop | Armor, repair allies, ally tanks | Backlog | 7 |
| Research | Extra Interest, Shell Research, Armor Research | MVP | 6 |
| Research | Toàn bộ 10 research tree | Backlog | 7 |
| Tank | Tank thường, light/heavy/kamikaze | MVP | 3/7 |
| Tank | Turret, siege, mine-layer, boss | Backlog | 7 |
| Environment | Wall, bush, spikes, terrain effects | MVP | 3/7 |
| Environment | Fog of war, destructible structures, radar | Backlog | 7 |
| HUD | HP, coins, stars, lives, enemy count, timer | MVP | 4/5 |
| UX | Pause, quick inventory, continue/exit confirmation | MVP | 6 |
| Leaderboard | Lưu và đọc kết quả từ PostgreSQL | MVP | 5/6 |
| Platform | Keyboard/mouse | MVP | 2/3 |
| Platform | Touch mode/mobile optimization | Backlog | 7+ |

## MVP tối thiểu để demo

1. Website mở được bằng browser.
2. Người chơi kết nối WebSocket và chơi được một trận.
3. Movement, shooting, collision, HP và AI hoạt động.
4. Có 3 màn campaign tuần tự.
5. Có mission briefing, victory/debriefing và reward.
6. Có autosave/load và leaderboard PostgreSQL.
7. Có một shop tối giản và một research tree tối giản.

Các tính năng còn lại được ghi nhận trong backlog, không làm ảnh hưởng deadline MVP.
