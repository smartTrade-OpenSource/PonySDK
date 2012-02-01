
package com.ponysdk.sample.client.datamodel;

public class Pony {

    private Long id;
    private String name;
    private Integer age;
    private String race;

    public Pony(final Long id, final String name, final Integer age, final String race) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.race = race;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getRace() {
        return race;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setAge(final Integer age) {
        this.age = age;
    }

    public void setRace(final String race) {
        this.race = race;
    }

}
