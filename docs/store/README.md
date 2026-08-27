# Store — Play assets

Source: [Play listing limits](https://support.google.com/googleplay/android-developer/answer/9866151) · [Feature graphic](https://support.google.com/googleplay/android-developer/answer/9866151)

Required before Play upload (even though GitHub is sideload today):

- Title `en-US/title.txt` ≤30 chars
- Short `short_desc.txt` ≤80
- Full `full_desc.txt` ≤4000
- Icon `icon-512.png` 512×512 PNG 32-bit, no alpha for Play (store listing)
- Feature `feature-1024x500.png` 1024×500 JPEG/PNG, no alpha
- Screenshots `phone-1.png`, `phone-2.png` at least 2, 1080px+, no device frame that misleads
- Privacy URL in `docs/PRIVACY.md` must be reachable

This folder is git-tracked; Play Console listing is copy-paste from here.
`scripts/qa/play-check.sh` checks title/short/full length if files exist (SKIP if placeholders missing).

Add real PNGs before upload; placeholders below are text specs.
