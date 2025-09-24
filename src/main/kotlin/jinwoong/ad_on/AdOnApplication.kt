package jinwoong.ad_on

import jinwoong.ad_on.schedule.application.service.SpentBudgetsMigrationService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class AdOnApplication

fun main(args: Array<String>) {
    val context = runApplication<AdOnApplication>(*args)
    val service = context.getBean(SpentBudgetsMigrationService::class.java)
    service.migrateSpentBudgets() // 한 번만 실행
}
