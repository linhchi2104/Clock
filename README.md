# Đồng hồ săn sale – Android (Bản nâng cấp v2.0 & Tự động Build APK)

Ứng dụng Android native (Kotlin) với đồng hồ nổi có thể kéo thả trên ứng dụng khác (Shopee, TikTok Shop, Lazada...).

## Các điểm đã hoàn thiện trong bản v2.0 này
1. **Sửa lỗi cập nhật tức thì (`ACTION_UPDATE`)**: Đã đăng ký `BroadcastReceiver` trong `FloatingClockService` để khi kéo thanh trượt chỉnh độ trong suốt, cỡ chữ hoặc bấm lưu giờ sale, đồng hồ nổi cập nhật ngay lập tức mà không cần tắt đi bật lại.
2. **Tương thích Android 13 & Android 14/15 (SDK 35)**: Bổ sung xin quyền thông báo (`POST_NOTIFICATIONS`) trên Android 13+ và truyền cờ `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` trong `startForeground()` để không bị văng ứng dụng (crash) trên Android 14/15.
3. **Đồng hồ xem trước (Preview) chạy thời gian thực**: Màn hình chính cập nhật đồng hồ mỗi 200ms thay vì đứng yên ở `19:11:08`.
4. **Tích hợp sẵn GitHub Actions (`.github/workflows/build-apk.yml`)**: Có thể xuất file `.apk` tự động trên đám mây mà không cần cài Android Studio nặng máy.

## Cách 1: Xuất file APK tự động trên GitHub (Không cần cài Android Studio)
1. Tạo một repository mới trên GitHub và tải toàn bộ các file trong thư mục này lên.
2. Vào tab **Actions** trên GitHub -> chọn workflow **Build FloatingSaleClock APK** -> nhấn **Run workflow**.
3. Sau khoảng 1–2 phút, tải file `FloatingSaleClock-debug-apk` (`app-debug.apk`) ở mục **Artifacts** về điện thoại và cài đặt.

## Cách 2: Build bằng Android Studio trên máy tính
1. Mở thư mục `FloatingSaleClock` bằng Android Studio (hỗ trợ AGP 8.6.1, JDK 17).
2. Chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. File APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.
