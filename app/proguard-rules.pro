# Add project-specific ProGuard rules here.
#
# This file is currently empty on purpose. sherpa-onnx ships an EMPTY
# consumer-rules.pro and resolves its classes and methods from native code by name
# (`Java_com_k2fsa_sherpa_onnx_*`), so an R8-minified build would strip or rename
# them and the JNI layer would fail at runtime. `app` therefore sets
# `isMinifyEnabled = false`, and turning it on requires keep rules for
# `com.k2fsa.sherpa.onnx.**` written and verified against a minified build first.
# See ADR-0007.