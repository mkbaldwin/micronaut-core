package io.micronaut.security

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.management.endpoint.health.HealthLevelOfDetail
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.UserDetails
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Singleton

class HealthSensitivitySpec extends Specification {

    @Unroll
    void "sensitive: #sensitive security: #security authenticated: #authenticated => #expected"(boolean sensitive,
                                                            boolean security,
                                                            boolean authenticated,
                                                            HealthLevelOfDetail expected) {
        given:
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, [
                'spec.name': 'healthsensitivity',
                'endpoints.health.enabled': true,
                'endpoints.health.sensitive': sensitive,
                'endpoints.health.disk-space.threshold': '9999GB',
                'micronaut.security.enabled': security,

        ])
        URL server = embeddedServer.getURL()
        RxHttpClient rxClient = embeddedServer.applicationContext.createBean(RxHttpClient, server)

        when:
        HttpRequest httpRequest = HttpRequest.GET("/health")
        if (authenticated) {
            httpRequest = httpRequest.basicAuth("user", "password")
        }
        def response = rxClient.exchange(httpRequest, Map).blockingFirst()
        Map result = response.body()

        then:
        response.code() == HttpStatus.OK.code
        result.status == "DOWN"
        if (expected == HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS ) {
            assert result.containsKey('details')
            assert result.details.diskSpace.status == "DOWN"
            assert result.details.diskSpace.details.error.startsWith("Free disk space below threshold.")
        } else {
            assert !result.containsKey('details')
        }

        cleanup:
        embeddedServer.close()
        rxClient.close()

        where:
        sensitive | security | authenticated || expected
        true      | true     | true          || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
        true      | true     | false         || HealthLevelOfDetail.STATUS
        false     | true     | false         || HealthLevelOfDetail.STATUS
        false     | true     | true          || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
        true      | false    | true          || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
        true      | false    | false         || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
        false     | false    | false         || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
        false     | false    | true          || HealthLevelOfDetail.STATUS_DESCRIPTION_DETAILS
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'healthsensitivity')
    static class AuthenticationProviderUserPassword implements AuthenticationProvider {

        @Override
        Publisher<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
            if ( authenticationRequest.identity == 'user' && authenticationRequest.secret == 'password' ) {
                return Flowable.just(new UserDetails('user', []))
            }
            return Flowable.just(new AuthenticationFailed())
        }
    }
}