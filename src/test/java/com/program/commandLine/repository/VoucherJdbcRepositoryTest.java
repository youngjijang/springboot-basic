package com.program.commandLine.repository;

import com.program.commandLine.model.customer.Customer;
import com.program.commandLine.model.customer.RegularCustomer;
import com.program.commandLine.model.voucher.FixedAmountVoucher;
import com.program.commandLine.model.voucher.Voucher;
import com.wix.mysql.EmbeddedMysql;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.util.UUID;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.ScriptResolver.classPathScript;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v8_0_11;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("release")
class VoucherJdbcRepositoryTest {

    @Configuration
    @ComponentScan(
            basePackages = {"com.program.commandLine.model.voucher", "com.program.commandLine.repository"}
    )
    static class Config {

        @Bean
        public DataSource dataSource() {
            HikariDataSource dataSource = DataSourceBuilder.create()
                    .url("jdbc:mysql://localhost:2215/test-voucher_mgmt")
                    .username("test")
                    .password("test1234!")
                    .type(HikariDataSource.class)
                    .build();
            return dataSource;
        }

    }

    @Autowired
    DataSource dataSource;

    @Autowired
    VoucherJdbcRepository voucherJdbcRepository;

    @Autowired
    CustomerJdbcRepository customerJdbcRepository;

    Customer customer;
    Voucher newVoucher;

    EmbeddedMysql embeddedMysql;

    @BeforeAll
    void setup() {
        var mysqlConfig = aMysqldConfig(v8_0_11)
                .withCharset(UTF8)
                .withPort(2215)
                .withUser("test", "test1234!")
                .withTimeZone("Asia/Seoul")
                .build();
        embeddedMysql = anEmbeddedMysql(mysqlConfig)
                .addSchema("test-voucher_mgmt", classPathScript("schema.sql"))
                .start();

        customer = customerJdbcRepository.insert(new RegularCustomer(UUID.randomUUID(), "test", "test@naver.com"));
        newVoucher = new FixedAmountVoucher(UUID.randomUUID(), 3000, false);
    }

    @AfterAll
    void cleanUp() {
        embeddedMysql.stop();
    }

    @Test
    @Order(1)
    @DisplayName("Hikari 연결 확인")
    public void testHikariConnectionPool() {
        assertThat(dataSource.getClass().getName(), is("com.zaxxer.hikari.HikariDataSource"));
    }

    @Test
    @Order(2)
    @DisplayName("바우처을 추가할 수 있다.")
    public void testInsert() {

        voucherJdbcRepository.insert(newVoucher);

        var retrievedCustomer = voucherJdbcRepository.findById(newVoucher.getVoucherId());
        assertThat(retrievedCustomer.isEmpty(), is(false));
        assertThat(retrievedCustomer.get(), samePropertyValuesAs(newVoucher));
    }


    @Test
    @Order(3)
    @DisplayName("전체 바우처를 조회할 수 있다.")
    public void testFindAll() {
        var vouchers = voucherJdbcRepository.findAll();
        assertThat(vouchers.isEmpty(), is(false));
    }


    @Test
    @Order(6)
    @DisplayName("바우처 정보를 변경할 수 있다.")
    public void testUpdateVoucher() {
        var beforeUsed = newVoucher.getUsed();

        newVoucher.used();
        var changed = voucherJdbcRepository.usedUpdate(newVoucher);

        assertThat(changed.getUsed(), not(beforeUsed));
    }

}