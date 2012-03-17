package de.skuzzle.polly.sdk;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>These class represents a Type for a signature. Each Types-instance represents a
 * type as well as its value.</p>
 * 
 * <p>For each type exists only one instance that represents only the type with no value
 * (for formal signatures). These isntances can be retrieved using the static factory
 * methods.</p>
 * 
 * @author Simon
 * @since zero day
 * @version RC 1.0
 */
public class Types {
    
    
    private static Map<Class<?>, Types> typeCache = new HashMap<Class<?>, Types>(10);

    /**
     * Factory method to create a formal command type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link CommandType}
     * @since 0.9
     */
    public static CommandType newCommand() {
        return newType(CommandType.class);
    }
    
    
    
    /**
     * Factory method to create a formal number type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link NumberType}
     * @since 0.9
     */
    public static NumberType newNumber() {
        return newType(NumberType.class);
    }
    
    
    
    /**
     * Factory method to create a formal date type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link DateType}
     * @since 0.9
     */
    public static DateType newDate() {
        return newType(DateType.class);
    }
    
    
    
    /**
     * Factory method to create a formal timespan type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link TimespanType}
     * @since 0.9
     */
    public static TimespanType newTimespan() {
        return newType(TimespanType.class);
    }
    
    
    
    /**
     * Factory method to create a formal boolean type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link BooleanType}
     * @since 0.9
     */
    public static BooleanType newBoolean() {
        return newType(BooleanType.class);
    }
    
    
    
    /**
     * Factory method to create a formal channel type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link ChannelType}
     * @since 0.9
     */
    public static ChannelType newChannel() {
        return newType(ChannelType.class);
    }
    
    
    
    /**
     * Factory method to create a formal user type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link UserType}
     * @since 0.9
     */
    public static UserType newUser() {
        return newType(UserType.class);
    }
    
    
    
    /**
     * Factory method to create a formal any type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link AnyType}
     * @since 0.9
     */
    public static AnyType newAny() {
        return newType(AnyType.class);
    }
    
    
    
    /**
     * Factory method to create a formal help type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link HelpType}
     * @since 0.9
     */
    public static HelpType newHelp() {
        return newType(HelpType.class);
    }
    
    
    
    /**
     * Factory method to create a formal string type (for formal signatures). This 
     * method will always return the same object.
     * 
     * @return A {@link StringType}
     * @since 0.9
     */
    public static StringType newString() {
        return newType(StringType.class);
    }
    
    
    
    
    private static synchronized <T> T newType(Class<T> cls) {
        Types t = typeCache.get(cls);
        if (t == null) {
            try {
                t = (Types) cls.newInstance();
            } catch (Exception e) {
                return null;
            }
            typeCache.put(cls, t);
        }
        return cls.cast(t);
    }
    
    
    
    private Types() {};
    
    
	
	/**
	 * This class represents a Number type. Note that a Number parameter matches any
	 * expression that evaluates to a number.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class NumberType extends Types {
		private double value;
		private int radix;
		
        /**
         * Creates a new Number-type with the given value and default radix = 10.
         * @param value The value of this type.
         */
        public NumberType(double value) {
            this(value, 10);
        }
		/**
		 * Creates a new Number-type with the given value and radix.
		 * @param value The value of this type.
		 * @param radix The radix of the value.
		 * @since 0.6.1
		 */
		public NumberType(double value, int radix) {
			this.value = value;
			this.radix = radix;
		}
		/**
		 * Creates a new Number-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected NumberType() {
			this(0.0, 10);
		}
		
		
		@Override
        public String getSample() {
            return "27.45";
        };
		
		
		/**
		 * Returns the number.
		 * @return the number.
		 */
		public double getValue() {
			return this.value;
		}
		
		
		/**
		 * Determines whether this is an integer number.
		 * @return <code>true</code> if this number is integer.
		 */
		public boolean isInteger() {
	        int val = (int) this.getValue();
	        return (double)val == this.getValue();
		}
	
		
		
		/**
		 * @return Returns this values String representation. If this is an integer 
		 *            number, the String is formatted in this numbers radix.
		 */
		@Override
		public String valueString(FormatManager formatter) {
		    if (this.isInteger()) {
		        return Integer.toString((int) this.getValue(), this.radix);
		    }
		    return formatter.formatNumber(this.value);
		}
		
		
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("Number");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a Boolean-type.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class BooleanType extends Types {
		private boolean value;
		
		
		/**
		 * Creates a new Boolean-type with the given value.
		 * @param value The value for this type.
		 */
		public BooleanType(boolean value) {
			this.value = value;
		}
		/**
		 * Creates a new Boolean-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected BooleanType() {
			this(false);
		}
		
		
		@Override
        public String getSample() {
            return "true";
        };
		
		
		/**
		 * Returns the boolean value. 
		 * @return The boolean value.
		 */
		public boolean getValue() {
			return this.value;
		}
		
		
        /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return Boolean.toString(this.value);
        }
		
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("Boolean");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a String-type.
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class StringType extends Types {
		private String value;
		
		
		/**
		 * Creates a new String-type with given value.
		 * @param value The string value.
		 */
		public StringType(String value) {
			this.value = value;
		}
		/**
		 * Creates a new String-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected StringType() {
			this("");
		}
		
		
		@Override
        public String getSample() {
            return "\"Nur ein Beispiel\"";
        };
		
		
		/**
		 * Returns the string.
		 * @return the string.
		 */
		public String getValue() {
			return this.value;
		}
		
		
        /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return this.value;
        }
        
        
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("String");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a Channel-type.
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class ChannelType extends Types {
		private String value;
		
		
		
		/**
		 * Creates a new Channel-type with the given channel. 
		 * @param value The channel of this type.
		 */
		public ChannelType(String value) {
			this.value = value;
		}
		/**
		 * Creates a new Channel-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected ChannelType() {
			this("");
		}
		
		
		@Override
        public String getSample() {
            return "#channel";
        };
		
		
		
		/**
		 * Returns the channel name.
		 * @return the channel name.
		 */
		public String getValue() {
			return this.value;
		}
		
	    /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return this.value;
        }
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("Channel");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a User-type.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class UserType extends Types {
		private String value;
		
		/**
		 * Creates a new User-type with the given user name.
		 * @param value The username.
		 */
		public UserType(String value) {
			this.value = value;
		}
		/**
		 * Creates a new User-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected UserType() {
			this("");
		}
		
		
		
		@Override
        public String getSample() {
            return "@Hans";
        };
		
		
		/**
		 * Returns the user name
		 * @return the user name.
		 */
		public String getValue() {
			return this.value;
		}
		
	    /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return this.value;
        }
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("User");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a Date-type.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class DateType extends Types {
		private Date value;
		
		/**
		 * Creates a new User-type with the given user name.
		 * @param value The username.
		 */
		public DateType(Date value) {
			this.value = value;
		}
		/**
		 * Creates a new Date-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected DateType() {
			this(new Date());
		}
		
		
		
		@Override
        public String getSample() {
            return "24.12.2012@17:30";
        };
		
		
		
		/**
		 * Returns the date.
		 * @return the date.
		 */
		public Date getValue() {
			return this.value;
		}
		
	    /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return formatter.formatDate(this.value);
        }
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("Date");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents a timespan-type. Timespans are fully compatible to
	 * {@link DateType}, but return another String when calling 
	 * {@link #valueString(FormatManager)}.
	 * 
	 * @author Simon
	 * @since Beta 0.2
	 */
	public static class TimespanType extends DateType {
	    private long span;
	    
	    /**
	     * Creates a new Timespan-Type.
	     * @param span The timespan in seconds.
	     * @param target The target date from now.
	     */
	    public TimespanType(long span, Date target) {
	        super(target);
	        this.span = span;
	    }
	    
	    protected TimespanType() {}

	    
	    /**
	     * Gets the timespan in seconds.
	     * @return The timespan.
	     */
	    public long getSpan() {
	        return this.span;
	    }
	    
	    
	    
	    @Override
	    public String getSample() {
	        return "8h22m7s";
	    };
	    
	    
	    
	    /**
	     * Formats this timespan as a string. Currently, it only returns the formatted
	     * target date, that is, this timespan added to the date where this instance 
	     * was created.
	     *  
	     * @param formatter The formatter which is used to format this timespan.
	     * @return The formatted String.
	     */
	    @Override
	    public String valueString(FormatManager formatter) {
	        return formatter.formatTimeSpan(this.span);
	    }
	    
	    
	    
	    public String toString() {
	        return "Timespan";
	    }
	}
	
	
	
	/**
	 * This class represents a Command-type.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class CommandType extends Types {
		private String value;
		
		/**
		 * Creates a new Command-type with the given user name.
		 * @param value The command name.
		 */
		public CommandType(String value) {
			this.value = value;
		}
		/**
		 * Creates a new Command-type with a default value. This may be used for
		 * formal signature parameters.
		 */
		protected CommandType() {
			this("");
		}
		
		
		
		/**
		 * Returns the command name.
		 * @return the command name.
		 */
		public String getValue() {
			return this.value;
		}
		
		
		
		@Override
		public String getSample() {
		    return ":sample";
		}
		
		
		
	    /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            return this.value;
        }
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("Command");
			if (withValue) {
				b.append(" (");
				b.append(this.value);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	
	/**
	 * This class represents a List of other types. It holds an element list of 
	 * {@link Types} which define this ListTypes subtype.
	 * 
	 * A ListType is only compatible to a ListType with the same subtype.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class ListType extends Types {
		private List<Types> elements;
		Types subtype;
		
		/**
		 * Creates a new ListType which holds the given values. The new ListTypes
		 * subType is determined by the first element of the List of values.
		 * 
		 * If the values-list is empty, the subType of this ListType is {@link AnyType}.
		 * @param values The elements of this ListType.
		 */
		public ListType(List<Types> values) {
			this.elements = values;
			this.subtype = new AnyType();
			if (!this.elements.isEmpty()) {
				this.subtype = this.elements.get(0);
			}
		}
		
		
		
		/**
		 * Creates a new empty ListType with the given Type as subType.
		 * @param subtype The subType of this {@link ListType}.
		 */
		public ListType(Types subtype) {
			this.elements = new ArrayList<Types>();
			this.subtype = subtype;
		}
		
		
		
		@Override
		public String getSample() {
		    return "{" + this.subtype.getSample() + "}"; 
		};
		
		
		/**
		 * Returns the elements of this ListType.
		 * @return The elements.
		 */
		public List<Types> getElements() {
			return this.elements;
		}
		
	    /**
         * @return Returns this values String representation.
         */
        @Override
        public String valueString(FormatManager formatter) {
            StringBuilder b = new StringBuilder();
            b.append("{");
            Iterator<Types> it = this.elements.iterator();
            while (it.hasNext()) {
                b.append(it.next().valueString(formatter));
                if (it.hasNext()) {
                    b.append(", ");
                }
            }
            b.append("}");
            return b.toString();
        }
		
		/**
		 * Checks this ListType against another type. Their are only considered 
		 * compatible if the other type is a ListType and their subTypes are compatible.
		 * @param other The type to check against this list type.
		 * @return <code>true</code> if the types are compatible, <code>false</code> 
		 * 		otherwise.
		 */
		public boolean check(Types other) {
			if (!(other instanceof ListType)) {
				return false;
			}
			ListType o = (ListType) other;
			return o.getElementType().check(this.getElementType());
		}
		
		
		
		/**
		 * Returns the subtype of this ListType.
		 * @return The subtype.
		 */
		public Types getElementType() {
			return this.subtype;
		}
		
		
		
		@Override
		public String toString() {
			return this.toString(false);
		}
		public String toString(boolean withValue) {
			StringBuilder b = new StringBuilder();
			b.append("List<");
			b.append(this.getElementType().toString());
			b.append(">");
			if (withValue) {
				b.append(" (");
				b.append(this.elements);
				b.append(")");
			}
			return b.toString();
		}
	}
	
	
	
	/**
	 * This class represents the Any type which has no value and is compatible to all
	 * other types.
	 * 
	 * @author Simon
     * @since zero day
     * @version RC 1.0
	 */
	public static class AnyType extends Types {
	    /**
	     * Creates a new AnyType with no value. Mind: value will remain <code>null</code>.
	     */
	    protected AnyType() {}

		@Override
		public String toString() {
			return "Any";
		}
		
		
		
		public String getSample() {
		    return "5.8";
		};
	}
	
	
	
	/**
	 * This class represents the help parameter type "?"
	 * 
	 * @author Simon
	 * @since 0.9
	 */
    public static class HelpType extends Types {
        
        protected HelpType() {}
        
        public String toString() {
            return "?";
        }
        
    }
	
    
    
	
	
	public String valueString(FormatManager formatter) {
	    return "";
	}
	
	
	
	/**
	 * Gets a valid sample string representation of this type. 
	 * 
	 * @return A String sample for this type.
	 */
	public String getSample() { return ""; };
	
	
	
	
	/**
	 * Checks if two types are compatible. The {@link AnyType} is compatible with
	 * every other type.
	 * Two types are considered compatible if their classes equals as returned by
	 * their {@link Object#getClass()} methods.
	 * 
	 * @param other The type to check this type against.
	 * @return <code>true</code> if the types are compatible, <code>false</code> 
	 * 		otherwise.
	 */
	public boolean check(Types other) {
	    if (other == this) {
	        return true;
	    } else if (other instanceof AnyType || this instanceof AnyType) {
			return true;
		}
		return this.getClass().isAssignableFrom(other.getClass());
	}
}