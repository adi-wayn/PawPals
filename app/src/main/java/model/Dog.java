package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Dog model matching the dog-details UI.
 * All fields are nullable so we save only what the user provided.
 */
@IgnoreExtraProperties
public class Dog implements Parcelable {

    // Basic
    private @Nullable String  name;
    private @Nullable String  breed;
    private @Nullable Integer age;        // nullable -> אפשר להשאיר ריק
    private @Nullable Boolean neutered;   // היה isSterilized

    // Extra details
    private @Nullable String personality;
    private @Nullable String mood;
    private @Nullable String notes;

    // Optional: picture URL in Storage / web
    private @Nullable String photoUrl;

    /** Required empty ctor for Firebase */
    public Dog() { }

    public Dog(@Nullable String name,
               @Nullable String breed,
               @Nullable Integer age,
               @Nullable Boolean neutered,
               @Nullable String personality,
               @Nullable String mood,
               @Nullable String notes,
               @Nullable String photoUrl) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.neutered = neutered;
        this.personality = personality;
        this.mood = mood;
        this.notes = notes;
        this.photoUrl = photoUrl;
    }

    // ------- Getters -------
    @Nullable public String  getName()       { return name; }
    @Nullable public String  getBreed()      { return breed; }
    @Nullable public Integer getAge()        { return age; }
    @Nullable public Boolean getNeutered()   { return neutered; }
    @Nullable public String  getPersonality(){ return personality; }
    @Nullable public String  getMood()       { return mood; }
    @Nullable public String  getNotes()      { return notes; }
    @Nullable public String  getPhotoUrl()   { return photoUrl; }

    // ------- Setters -------
    public void setName(@Nullable String name)             { this.name = name; }
    public void setBreed(@Nullable String breed)           { this.breed = breed; }
    public void setAge(@Nullable Integer age)              { this.age = age; }      // Integer כדי לאפשר null
    public void setNeutered(@Nullable Boolean neutered)    { this.neutered = neutered; }
    public void setPersonality(@Nullable String personality){ this.personality = personality; }
    public void setMood(@Nullable String mood)             { this.mood = mood; }
    public void setNotes(@Nullable String notes)           { this.notes = notes; }
    public void setPhotoUrl(@Nullable String photoUrl)     { this.photoUrl = photoUrl; }

    // ------- Firestore map -------
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        if (name != null)        m.put("name", name);
        if (breed != null)       m.put("breed", breed);
        if (age != null)         m.put("age", age);
        if (neutered != null)    m.put("neutered", neutered);
        if (personality != null) m.put("personality", personality);
        if (mood != null)        m.put("mood", mood);
        if (notes != null)       m.put("notes", notes);
        if (photoUrl != null)    m.put("photoUrl", photoUrl);
        return m;
    }

    /** Helper: build from Firestore map safely */
    public static Dog fromMap(@Nullable Map<String, Object> map) {
        Dog d = new Dog();
        if (map == null) return d;
        Object v;
        v = map.get("name");        if (v instanceof String)  d.setName((String) v);
        v = map.get("breed");       if (v instanceof String)  d.setBreed((String) v);
        v = map.get("age");         if (v instanceof Number)  d.setAge(((Number) v).intValue());
        v = map.get("neutered");    if (v instanceof Boolean) d.setNeutered((Boolean) v);
        v = map.get("personality"); if (v instanceof String)  d.setPersonality((String) v);
        v = map.get("mood");        if (v instanceof String)  d.setMood((String) v);
        v = map.get("notes");       if (v instanceof String)  d.setNotes((String) v);
        v = map.get("photoUrl");    if (v instanceof String)  d.setPhotoUrl((String) v);
        return d;
    }

    // ------- Parcelable -------
    protected Dog(Parcel in) {
        name        = in.readString();
        breed       = in.readString();
        age         = (Integer) in.readValue(Integer.class.getClassLoader());
        neutered    = (Boolean) in.readValue(Boolean.class.getClassLoader());
        personality = in.readString();
        mood        = in.readString();
        notes       = in.readString();
        photoUrl    = in.readString();
    }

    public static final Creator<Dog> CREATOR = new Creator<Dog>() {
        @Override public Dog createFromParcel(Parcel in) { return new Dog(in); }
        @Override public Dog[] newArray(int size) { return new Dog[size]; }
    };

    @Override public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(breed);
        dest.writeValue(age);        // handles null
        dest.writeValue(neutered);   // handles null
        dest.writeString(personality);
        dest.writeString(mood);
        dest.writeString(notes);
        dest.writeString(photoUrl);
    }

    @Override
    public String toString() {
        return "Dog{" +
                "name='" + name + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + age +
                ", neutered=" + neutered +
                ", personality='" + personality + '\'' +
                ", mood='" + mood + '\'' +
                ", notes='" + notes + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                '}';
    }
}
