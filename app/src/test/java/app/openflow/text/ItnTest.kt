package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ItnTest {

    // ---- ItnDateTime: dates ----
    @Test
    fun month_ordinal() {
        assertThat(ItnDateTime.apply("may third")).isEqualTo("May 3")
        assertThat(ItnDateTime.apply("february fourteenth")).isEqualTo("February 14")
        assertThat(ItnDateTime.apply("december twenty fifth")).isEqualTo("December 25")
    }

    @Test
    fun month_day_year() {
        assertThat(ItnDateTime.apply("january first twenty twenty four"))
            .isEqualTo("January 1, 2024")
        assertThat(ItnDateTime.apply("march second twenty twenty five"))
            .isEqualTo("March 2, 2025")
    }

    @Test
    fun ordinal_of_month_and_bare() {
        assertThat(ItnDateTime.apply("the tenth of december"))
            .isEqualTo("the 10th of December")
        assertThat(ItnDateTime.apply("on the twenty third")).isEqualTo("on the 23rd")
        assertThat(ItnDateTime.apply("see you on the first"))
            .isEqualTo("see you on the 1st")
    }

    // ---- ItnDateTime: times ----
    @Test
    fun hour_minute_meridiem() {
        assertThat(ItnDateTime.apply("four thirty pm")).isEqualTo("4:30 PM")
        assertThat(ItnDateTime.apply("wake me at six thirty am"))
            .isEqualTo("wake me at 6:30 AM")
        assertThat(ItnDateTime.apply("it's three thirty")).isEqualTo("it's 3:30")
        assertThat(ItnDateTime.apply("ten fifteen in the morning"))
            .isEqualTo("10:15 AM")
    }

    @Test
    fun past_to_oclock() {
        assertThat(ItnDateTime.apply("half past five")).isEqualTo("5:30")
        assertThat(ItnDateTime.apply("quarter past nine")).isEqualTo("9:15")
        assertThat(ItnDateTime.apply("a quarter to six")).isEqualTo("a 5:45")
        assertThat(ItnDateTime.apply("ten to eleven")).isEqualTo("10:50")
        assertThat(ItnDateTime.apply("nine o'clock")).isEqualTo("9:00")
    }

    @Test
    fun at_hour() {
        assertThat(ItnDateTime.apply("the train leaves at seven"))
            .isEqualTo("the train leaves at 7:00")
        assertThat(ItnDateTime.apply("we start at eight sharp"))
            .isEqualTo("we start at 8:00 sharp")
    }

    @Test
    fun noon_midnight_untouched() {
        assertThat(ItnDateTime.apply("lunch at noon")).isEqualTo("lunch at noon")
        assertThat(ItnDateTime.apply("we met at midnight")).isEqualTo("we met at midnight")
    }

    // ---- ItnMoney ----
    @Test
    fun dollars() {
        assertThat(ItnMoney.apply("twenty five dollars")).isEqualTo("$25")
        assertThat(ItnMoney.apply("one hundred dollars")).isEqualTo("$100")
        assertThat(ItnMoney.apply("two thousand dollars")).isEqualTo("$2,000")
        assertThat(ItnMoney.apply("five dollars and fifty cents")).isEqualTo("$5.50")
        assertThat(ItnMoney.apply("a ten dollar bill")).isEqualTo("a $10 bill")
        assertThat(ItnMoney.apply("fifty cents")).isEqualTo("$0.50")
    }

    @Test
    fun other_currencies() {
        assertThat(ItnMoney.apply("ten euros")).isEqualTo("10 euros")
        assertThat(ItnMoney.apply("it costs twelve pounds")).isEqualTo("it costs £12")
        assertThat(ItnMoney.apply("i paid thirty bucks")).isEqualTo("i paid 30 bucks")
    }

    @Test
    fun implied_price() {
        assertThat(ItnMoney.apply("the total is nine ninety nine"))
            .isEqualTo("the total is $9.99")
    }

    // ---- ItnElectronic ----
    @Test
    fun emails() {
        assertThat(ItnElectronic.apply("email me at john at gmail dot com"))
            .isEqualTo("email me at john@gmail.com")
        assertThat(ItnElectronic.apply("reach out to sarah at openflow dot org"))
            .isEqualTo("reach out to sarah@openflow.org")
        assertThat(ItnElectronic.apply("contact me at support at company dot com"))
            .isEqualTo("contact me at support@company.com")
    }

    @Test
    fun urls_and_domains() {
        assertThat(ItnElectronic.apply("the site is www dot example dot com"))
            .isEqualTo("the site is www.example.com")
        assertThat(ItnElectronic.apply("the url is example dot com slash docs"))
            .isEqualTo("the url is example.com/docs")
        assertThat(ItnElectronic.apply("visit openflow dot io slash docs"))
            .isEqualTo("visit openflow.io/docs")
    }

    @Test
    fun ips_and_paths() {
        assertThat(ItnElectronic.apply("the server is at ten dot one dot two dot five"))
            .isEqualTo("the server is at 10.1.2.5")
        assertThat(
            ItnElectronic.apply("the file is at home slash user slash docs slash report dot pdf")
        ).isEqualTo("the file is at /home/user/docs/report.pdf")
    }

    @Test
    fun phones() {
        assertThat(ItnElectronic.apply("call nine one one")).isEqualTo("call 911")
        assertThat(ItnElectronic.apply("my number is five five five one two three four"))
            .isEqualTo("my number is 555-1234")
        assertThat(
            ItnElectronic.apply(
                "my phone is two one two five five five zero one zero zero"
            )
        ).isEqualTo("my phone is 212-555-0100")
    }

    // ---- Full facade ordering ----
    @Test
    fun facade_end_to_end() {
        assertThat(
            Itn.apply("email me at john at gmail dot com about twenty five dollars")
        ).isEqualTo("email me at john@gmail.com about $25")
    }
}
