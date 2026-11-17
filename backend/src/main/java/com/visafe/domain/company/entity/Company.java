package com.visafe.domain.company.entity;

import com.visafe.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A. 사업주 (companies). 로그인·소속을 위한 계정 정보이며, 계약서에 적힌 사업체 정보는
 * {@code contract_labor_terms}에 따로 저장한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "companies",
        uniqueConstraints = {@UniqueConstraint(name = "uq_companies__business_reg_no", columnNames = "business_reg_no")
        })
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사업체명 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 대표자 */
    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    /** 사업자등록번호 (123-45-67890) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "business_reg_no", nullable = false, length = 12)
    private String businessRegNo;

    /** 사업장 주소 */
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    private Company(String name, String representativeName, String businessRegNo, String address) {
        this.name = name;
        this.representativeName = representativeName;
        this.businessRegNo = businessRegNo;
        this.address = address;
    }

    public static Company create(String name, String representativeName, String businessRegNo, String address) {
        return new Company(name, representativeName, businessRegNo, address);
    }

    public void update(String name, String representativeName, String address) {
        this.name = name;
        this.representativeName = representativeName;
        this.address = address;
    }
}
