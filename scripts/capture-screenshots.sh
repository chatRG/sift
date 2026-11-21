#!/usr/bin/env bash
# Capture README screenshots from a booted emulator.
#
# Runs inside reactivecircus/android-emulator-runner on a GitHub-hosted
# runner (which has /dev/kvm). The app is installed fresh, permissions and
# the default-SMS role are granted over adb so the system dialogs never
# appear, then the onboarding is tapped through with uiautomator and a small
# corpus of SMS is injected through the emulator console.
set -euo pipefail

PKG="app.sift.sms"
ACTIVITY="app.sift.sms/app.clearsms.MainActivity"
APK="app/build/outputs/apk/debug/Sift-debug.apk"
OUT="docs/screenshots"
TAP="python3 scripts/ui_tap.py"

mkdir -p "$OUT"

echo "== device =="
adb wait-for-device
adb shell wm size 1080x2160
adb shell wm density 420
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "== install =="
adb install -r "$APK"

echo "== grant permissions and the SMS role =="
for perm in READ_SMS RECEIVE_SMS SEND_SMS RECEIVE_MMS READ_CONTACTS POST_NOTIFICATIONS; do
  adb shell pm grant "$PKG" "android.permission.$perm" 2>/dev/null || true
done
adb shell cmd role add-role-holder android.app.role.SMS "$PKG" 2>/dev/null || true

echo "== launch and finish onboarding =="
adb shell am start -n "$ACTIVITY"
sleep 6
$TAP "Get started" || true
sleep 2
$TAP "Skip" || true
sleep 2
$TAP "Skip" || true
sleep 2
$TAP "Done" || true
sleep 5

echo "== inject SMS =="
send() {
  adb emu sms send "$1" "$2" || echo "sms send failed for $1"
  sleep 2
}
send "VM-HDFCBK-T" "Rs.1,250.00 debited from A/c XX4821 on 21-11-26 to SWIGGY. Avl bal Rs.48,320.15 - HDFC Bank"
send "VK-OTPSMS" "482913 is your OTP for login. Valid for 10 minutes. Do not share it with anyone."
send "AD-SHOPPY-P" "Flat 40% off on electronics this weekend. Offer ends tonight. Shop now at https://shop.example/x1 . T&C apply."
send "AD-LOANCO-P" "Pre-approved personal loan of Rs.5,00,000. Apply now at https://loans.example/apply or call 1800123456. T&C apply."
send "VK-POWERCO" "Your electricity bill of Rs.1,140 has been generated. Pay before 28-11-26 to avoid a late fee."
send "+919876543210" "Reached home safely. Will call you in the evening."
sleep 4

shot() {
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "captured $OUT/$1.png"
}

echo "== captures =="
adb shell am start -n "$ACTIVITY"
sleep 3
shot inbox

# The Spam pill's own view, with its "Empty spam" banner.
$TAP "Spam" --exact || true
sleep 3
shot spam

# Finance dashboard, built from the bank SMS above.
$TAP "Finance" --exact || true
sleep 3
shot finance

# A conversation: the HDFC thread opened from the inbox.
$TAP "Inbox" --exact || true
sleep 2
$TAP "HDFC" || $TAP "48,320" || adb shell input tap 540 560
sleep 3
shot conversation
adb shell input keyevent KEYCODE_BACK

echo "== done =="
ls -lh "$OUT"
