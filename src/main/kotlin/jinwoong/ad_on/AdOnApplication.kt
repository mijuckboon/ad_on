package jinwoong.ad_on

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class AdOnApplication

fun main(args: Array<String>) {
    runApplication<AdOnApplication>(*args)
}
