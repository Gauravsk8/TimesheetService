package com.example.timesheet.generator;

import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.query.NativeQuery;

public abstract class AbstractDocNoGenerator implements IdentifierGenerator {

    protected abstract String dbFunction();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        NativeQuery<String> q = session.createNativeQuery("SELECT " + dbFunction());
        return q.getSingleResult();
    }
}
