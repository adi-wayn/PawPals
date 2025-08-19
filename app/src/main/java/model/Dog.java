package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Dog model matching the dog-details UI.
 * Fields are mostly nullable so it's easy to save only what the user filled.
 */
public class Dog implements Parcelable {

    // Basic
    private @Nullable String name;
    private @Nullable String breed;
    private @Nullable Integer age;          // nullable -> אפשר להשאיר ריק
    private @Nullable Boolean neutered;     // היה isSterilized

    // Extra details shown in the screen
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
    @Nullable public String getName() { return name; }
    @Nullable public String getBreed() { return breed; }
    @Nullable public Integer getAge() { return age; }
    @Nullable public Boolean getNeutered() { return neutered; } // היה isSterilized
    @Nullable public String getPersonality() { return personality; }
    @Nullable public String getMood() { return mood; }
    @Nullable public String getNotes() { return notes; }
    @Nullable public String getPhotoUrl() { return photoUrl; }

    // ------- Setters -------
    public void setName(@Nullable String name) { this.name = name; }
    public void setBreed(@Nullable String breed) { this.breed = breed; }
    public void setAge(@Nullable Integer age) { this.age = age; }
    public void setNeutered(@Nullable Boolean neutered) { this.neutered = neutered; }
    public void setPersonality(@Nullable String personality) { this.personality = personality; }
    public void setMood(@Nullable String mood) { this.mood = mood; }
    public void setNotes(@Nullable String notes) { this.notes = notes; }
    public void setPhotoUrl(@Nullable String photoUrl) { this.photoUrl = photoUrl; }

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

    // ------- Parcelable -------
    protected Dog(Parcel in) {
        name = in.readString();
        breed = in.readString();
        age = (Integer) in.readValue(Integer.class.getClassLoader());
        neutered = (Boolean) in.readValue(Boolean.class.getClassLoader());
        personality = in.readString();
        mood = in.readString();
        notes = in.readString();
        photoUrl = in.readString();
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
}
