package com.github.jeremiehuchet.httpend2endencryption

import com.github.jeremiehuchet.httpend2endencryption.http.EncryptedContentEncodingFilter
import com.github.jeremiehuchet.httpend2endencryption.http.HttpEncryptedProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean

@SpringBootApplication
@ConfigurationPropertiesScan
class HttpEnd2endEncryptionApplication {

    @Bean
    fun filterRegistrationBean(config: HttpEncryptedProperties) = FilterRegistrationBean<EncryptedContentEncodingFilter>()
        .apply {
            filter = EncryptedContentEncodingFilter(config)
            addUrlPatterns("/encrypted/*")
        }
}

fun main(args: Array<String>) {
    runApplication<HttpEnd2endEncryptionApplication>(*args)
}
