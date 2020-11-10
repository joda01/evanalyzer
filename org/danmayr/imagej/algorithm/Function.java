package algorithm;

public enum Function {
    noSelection ("--No selection--"),
    calcColoc ("Calc Colocalization"),
    countExosomes ("Count Exosomes");

    private final String name;       

    private Function(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        // (otherName == null) check is not needed because name.equals(null) returns false 
        return name.equals(otherName);
    }

    public String toString() {
       return this.name;
    }
}