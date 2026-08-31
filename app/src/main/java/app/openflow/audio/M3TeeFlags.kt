package app.openflow.audio

/**
 * M3-A feature flag. Starts false for rollback per rev2 spec.
 * Flip to true only after unit + device matrix + no orphan + generation invariant pass.
 */
object M3TeeFlags {
    @Volatile var USE_TEE: Boolean = false
}
