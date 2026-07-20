# Cnotifi: Công Cụ Thông Báo Android Tiên Tiến

**Cnotifi** là ứng dụng Android mạnh mẽ cho phép bạn **tạo, quản lý và gửi thông báo local** với độ tùy chỉnh cực cao. Dành cho developer, tester và power user muốn kiểm soát hoàn toàn hành vi thông báo trên thiết bị.

## Tính năng nổi bật

- **Xây dựng thông báo linh hoạt**: Tùy chỉnh title, content, icon (hàng loạt icon SVG đẹp mắt), large icon từ ảnh thiết bị.
- **Quản lý asset**: Lưu trữ và xử lý hình ảnh (PNG) trực tiếp trong app.
- **Dynamic Shortcuts**: Tạo shortcut trên màn hình chính để trigger thông báo nhanh chóng.
- **Notification Sequence**: Hỗ trợ gửi chuỗi thông báo theo lịch hoặc trigger.
- **Jetpack Compose UI**: Giao diện hiện đại, mượt mà với Material Design 3.
- **Room Database + Repository pattern**: Quản lý dữ liệu thông báo chuyên nghiệp.

## Công nghệ sử dụng

- **Ngôn ngữ**: Kotlin 100%
- **UI**: Jetpack Compose + Material 3
- **Kiến trúc**: MVVM với ViewModel, StateFlow, Coroutines
- **Database**: Room + DAO
- **Dependency Injection & Build**: Gradle KTS + Version Catalog
- **Hình ảnh**: Coil cho loading & caching

## Hướng dẫn cài đặt & Build

1. Clone repo:
   ```bash
   git clone https://github.com/Anhdeface/Cnotifi.git
   ```
2. Mở project trong Android Studio.
3. Sync Gradle.
4. Chạy trên emulator hoặc thiết bị thật (API 24+).

## Screenshots & Demo
(Thêm ảnh demo ở đây sau)

## Mục đích
Công cụ hoàn hảo để test notification behavior, xây dựng automation, hoặc tạo trải nghiệm thông báo độc đáo trên Android.

---

**Tác giả**: Anhdeface (xounzii)  
**License**: MIT (hoặc tùy chỉnh theo nhu cầu)

Nếu bạn thấy hữu ích, hãy **star** repo và contribute ý tưởng! 🚀