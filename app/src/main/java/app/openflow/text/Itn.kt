package app.openflow.text

/**
 * ITN orchestration: electronic forms first (they need digit words intact),
 * then dates/times/money, generic numbers last.
 */
object Itn {
    fun apply(t: String): String =
        ItnNumber.apply(
            ItnMoney.apply(
                ItnDateTime.apply(
                    ItnElectronic.apply(t)
                )
            )
        )
}
