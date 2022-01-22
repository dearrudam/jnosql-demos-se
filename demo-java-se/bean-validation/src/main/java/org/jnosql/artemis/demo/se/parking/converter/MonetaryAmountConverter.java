/*
 * Copyright (c) 2019 Otávio Santana and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Otavio Santana
 */
package org.jnosql.artemis.demo.se.parking.converter;

import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.types.Decimal128;
import org.javamoney.moneta.Money;
import org.eclipse.jnosql.mapping.AttributeConverter;
import org.eclipse.jnosql.communication.api.document.Document;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class MonetaryAmountConverter implements AttributeConverter<MonetaryAmount, Object> {


    private static final String VALUE = "value";
    private static final String CURRENCY = "currency";
    private static final String DEFAULT_CURRENCY = "USD";

    @Override
    public BsonDocument convertToDatabaseColumn(MonetaryAmount attribute) {
        if (attribute == null) {
            return null;
        }

        String currency = attribute.getCurrency().getCurrencyCode();
        Decimal128 value = new Decimal128(attribute.getNumber().numberValue(BigDecimal.class));

        BsonDocument document = new BsonDocument();
        document.append(CURRENCY, new BsonString(currency));
        document.append(VALUE, new BsonDecimal128(value));
        return document;
    }

    @Override
    public MonetaryAmount convertToEntityAttribute(Object dbData) {

        if (dbData == null) {
            return null;
        }

        if (dbData instanceof BsonDocument) {
            return getMonetaryAmount(BsonDocument.class.cast(dbData));
        }
        return getMonetaryAmount(List.class.cast(dbData));

    }

    private MonetaryAmount getMonetaryAmount(BsonDocument dbData) {
        BigDecimal value = Optional.ofNullable(dbData.get(VALUE))
                .map(BsonValue::asDecimal128)
                .map(BsonDecimal128::decimal128Value)
                .map(Decimal128::bigDecimalValue)
                .orElse(BigDecimal.ZERO);

        String currency = Optional.ofNullable(dbData.get(CURRENCY))
                .map(BsonValue::asString)
                .map(BsonString::getValue)
                .orElse(DEFAULT_CURRENCY);

        return Money.of(value, currency);
    }

    private MonetaryAmount getMonetaryAmount(List<Document> dbData) {

        BigDecimal value = dbData.stream()
                .filter(d -> VALUE.equals(d.getName()))
                .findFirst()
                .map(d -> d.get(Decimal128.class).bigDecimalValue())
                .orElse(BigDecimal.ZERO);

        String currency = dbData.stream()
                .filter(d -> CURRENCY.equals(d.getName()))
                .findFirst()
                .map(d -> d.get(String.class))
                .orElse(DEFAULT_CURRENCY);

        return Money.of(value, currency);
    }
}