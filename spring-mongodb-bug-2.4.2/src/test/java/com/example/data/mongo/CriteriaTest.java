package com.example.data.mongo;

import lombok.EqualsAndHashCode;
import lombok.ToString;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UntypedExampleMatcher;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataMongoTest
public class CriteriaTest {

    Person p1, p2, p3;

    @Autowired
    public MongoTemplate operations;

    @BeforeEach
    public void setUp() {

        operations.getCollection("dramatis-personae").deleteMany(new org.bson.Document());

        p1 = new Person();
        p1.firstname = "bran";
        p1.middlename = "a";
        p1.lastname = "stark";

        p2 = new Person();
        p2.firstname = "jon";
        p2.lastname = "snow";

        p3 = new Person();
        p3.firstname = "arya";
        p3.lastname = "stark";

        operations.save(p1);
        operations.save(p2);
        operations.save(p3);
    }

    @Test
    public void myExampleTest() {
        Person sample = new Person();
        sample.firstname = "bran";
        Criteria criteria = new Criteria().alike(Example.of(sample));
        criteria.andOperator(new Criteria("last_name").is("stark"));
        Query query = new Query(criteria);
        List<Person> result = operations.find(query, Person.class);
        assertThat(result).containsExactlyInAnyOrder(p1);

    }

    @Test
    public void myExampleTest1() {
        Person sample = new Person();
        sample.firstname = "bran";
        Criteria criteria = new Criteria().alike(Example.of(sample));
        Query query = new Query(criteria).addCriteria(new Criteria("last_name").is("stark"));
        List<Person> result = operations.find(query, Person.class);
        assertThat(result).containsExactlyInAnyOrder(p1);

    }

    @Test // DATAMONGO-1245
    public void findByExampleShouldWorkForSimpleProperty() {

        Person sample = new Person();
        sample.lastname = "stark";

        Query query = new Query(new Criteria().alike(Example.of(sample)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(p1, p3);
    }

    @Test // DATAMONGO-1245
    public void findByExampleShouldWorkForMultipleProperties() {

        Person sample = new Person();
        sample.lastname = "stark";
        sample.firstname = "arya";

        Query query = new Query(new Criteria().alike(Example.of(sample)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactly(p3);
    }

    @Test // DATAMONGO-1245
    public void findByExampleShouldWorkForIdProperty() {

        Person p4 = new Person();
        operations.save(p4);

        Person sample = new Person();
        sample.id = p4.id;

        Query query = new Query(new Criteria().alike(Example.of(sample)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactly(p4);
    }

    @Test // DATAMONGO-1245
    public void findByExampleShouldReturnEmptyListIfNotMatching() {

        Person sample = new Person();
        sample.firstname = "jon";
        sample.firstname = "stark";

        Query query = new Query(new Criteria().alike(Example.of(sample)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).isEmpty();
    }

    @Test // DATAMONGO-1245
    public void findByExampleShouldReturnEverythingWhenSampleIsEmpty() {

        Person sample = new Person();

        Query query = new Query(new Criteria().alike(Example.of(sample)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(p1, p2, p3);
    }

    @Test // DATAMONGO-1245
    public void findByExampleWithCriteria() {

        Person sample = new Person();
        sample.lastname = "stark";

        Query query = new Query(new Criteria().alike(Example.of(sample)).and("firstname").regex("^ary*"));

        List<Person> result = operations.find(query, Person.class);
        assertThat(result).hasSize(1);
    }

    @Test // DATAMONGO-1459
    public void findsExampleUsingAnyMatch() {

        Person probe = new Person();
        probe.lastname = "snow";
        probe.middlename = "a";

        Query query = Query.query(Criteria.byExample(Example.of(probe, ExampleMatcher.matchingAny())));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(p1, p2);
    }

    @Test // DATAMONGO-1768
    public void typedExampleMatchesNothingIfTypesDoNotMatch() {

        NotAPersonButStillMatchingFields probe = new NotAPersonButStillMatchingFields();
        probe.lastname = "stark";

        Query query = new Query(new Criteria().alike(Example.of(probe)));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).isEmpty();
    }

    @Test // DATAMONGO-1768
    public void exampleIgnoringClassTypeKeyMatchesCorrectly() {

        NotAPersonButStillMatchingFields probe = new NotAPersonButStillMatchingFields();
        probe.lastname = "stark";

        Query query = new Query(
            new Criteria().alike(Example.of(probe, ExampleMatcher.matching().withIgnorePaths("_class"))));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(p1, p3);
    }

    @Test // DATAMONGO-1768
    public void untypedExampleMatchesCorrectly() {

        NotAPersonButStillMatchingFields probe = new NotAPersonButStillMatchingFields();
        probe.lastname = "stark";

        Query query = new Query(new Criteria().alike(Example.of(probe, UntypedExampleMatcher.matching())));
        List<Person> result = operations.find(query, Person.class);

        assertThat(result).containsExactlyInAnyOrder(p1, p3);
    }

    @Test // DATAMONGO-2314
    public void alikeShouldWorkOnNestedProperties() {

        PersonWrapper source1 = new PersonWrapper();
        source1.id = "with-child-doc-1";
        source1.child = p1;

        PersonWrapper source2 = new PersonWrapper();
        source2.id = "with-child-doc-2";
        source2.child = p2;

        operations.save(source1);
        operations.save(source2);

        Query query = new Query(
            new Criteria("child").alike(Example.of(p1, ExampleMatcher.matching().withIgnorePaths("_class"))));
        List<PersonWrapper> result = operations.find(query, PersonWrapper.class);

        assertThat(result).containsExactly(source1);
    }

    @Document("dramatis-personae")
    @EqualsAndHashCode
    @ToString
    static class Person {

        @Id
        String id;
        String firstname, middlename;
        @Field("last_name")
        String lastname;
    }

    @EqualsAndHashCode
    @ToString

    static class NotAPersonButStillMatchingFields {

        String firstname, middlename;
        @Field("last_name")
        String lastname;
    }

    @Document("dramatis-personae")
    @EqualsAndHashCode
    @ToString
    static class PersonWrapper {

        @Id
        String id;
        Person child;
    }

}
