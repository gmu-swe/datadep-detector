package edu.gmu.swe.datadep;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.jdom2.Element;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import edu.gmu.swe.datadep.xstream.ReferenceByXPathWithDependencysMarshaller;
import edu.gmu.swe.datadep.xstream.WrappedPrimitiveConverter;

public class HeapWalker {
	protected static String getFieldFQN(Field f) {
		String clz = f.getDeclaringClass().getName();
		String fld = f.getName();
		return clz + "." + fld;
	}

	public static Object loggerSingleton;
	private static final LinkedList<StaticField> sfPool = new LinkedList<StaticField>();
	private static final Set<String> whiteList;

	public static void clearWhitelist() {
		whiteList.clear();
	}

	public static void addToWhitelist(String s) {  
		whiteList.add(s);
	}

	// private static final Set<String> ignores;

	private static Set<String> fileToSet(Properties p, String key) {
		String f = p.getProperty(key);
		if (f == null) {
			System.err.println("Warn: Whitelist not specified");
			return new HashSet<String>();
		}
		if (new File(f).exists()) {
			HashSet<String> ret = new HashSet<String>();
			Scanner s;
			try {
				s = new Scanner(new File(f));
				while (s.hasNextLine()) {
					ret.add(s.nextLine());
				}
				s.close();
			} catch (FileNotFoundException e) {
			}
			return ret;
		} else
			throw new IllegalArgumentException("Provided whitelist file, " + f + ", does not exist");
	}

	static {
		whiteList = fileToSet(System.getProperties(), "whitelist");
		System.out.println("Loaded whitelist");
		// ignores = fileToSet(System.getProperties(), "ignores");
	}

	protected static boolean shouldCapture(Field f) {
		if (f.getDeclaringClass().getName().startsWith("java") || f.getDeclaringClass().getName().startsWith("sun") || f.getDeclaringClass().getName().startsWith("edu.gmu.swe.datadep."))
			return false;
		String fieldName = getFieldFQN(f);
		String fldLower = fieldName.toLowerCase();
		if ((fldLower.contains("mockito")) || (fldLower.contains("$$"))) {
			// System.out.println("***Ignored_Root: " + fieldName);
			return false;
		}

		if (whiteList.isEmpty())
			return true;
		Package p = f.getDeclaringClass().getPackage();
		if (p != null) {
			String pkg = p.getName();
			for (String s : whiteList) {
				if (pkg.startsWith(s))
					return true;
			}
		}

		return false;
	}

	private static boolean isBlackListedSF(Field f) {
		String className = f.getDeclaringClass().getName();
		String fieldName = f.getName();

		return "java.lang.reflect.Field".equals(className) || "java.lang.reflect.Method".equals(className) || "java.lang.Class".equals(className) || ("java.lang.System".equals(className) && "out".equals(fieldName))
				|| ("java.io.BufferedInputStream".equals(className) && "bufUpdater".equals(fieldName)) || ("java.io.BufferedReader".equals(className) && "defaultExpectedLineLength".equals(fieldName)) || ("java.io.File".equals(className) && "separatorChar".equals(fieldName))
				|| ("java.io.File".equals(className) && "fs".equals(fieldName)) || ("java.io.File".equals(className) && "separator".equals(fieldName)) || ("java.io.File$PathStatus".equals(className) && "CHECKED".equals(fieldName))
				|| ("java.io.File$PathStatus".equals(className) && "INVALID".equals(fieldName)) || ("java.io.FileDescriptor".equals(className) && "err".equals(fieldName)) || ("java.io.FileDescriptor".equals(className) && "out".equals(fieldName))
				|| ("java.io.FileDescriptor".equals(className) && "in".equals(fieldName)) || ("java.io.ObjectInputStream".equals(className) && "unsharedMarker".equals(fieldName)) || ("java.io.ObjectOutputStream".equals(className) && "extendedDebugInfo".equals(fieldName))
				|| ("java.io.ObjectStreamClass".equals(className) && "reflFactory".equals(fieldName)) || ("java.io.ObjectStreamClass".equals(className) && "NO_FIELDS".equals(fieldName)) || ("java.io.ObjectStreamClass$Caches".equals(className) && "reflectors".equals(fieldName))
				|| ("java.io.ObjectStreamClass$Caches".equals(className) && "reflectorsQueue".equals(fieldName)) || ("java.io.ObjectStreamClass$Caches".equals(className) && "localDescs".equals(fieldName))
				|| ("java.io.ObjectStreamClass$Caches".equals(className) && "localDescsQueue".equals(fieldName)) || ("java.io.ObjectStreamClass$EntryFuture".equals(className) && "unset".equals(fieldName))
				|| ("java.io.ObjectStreamClass$FieldReflector".equals(className) && "unsafe".equals(fieldName)) || ("java.lang.annotation.RetentionPolicy".equals(className) && "RUNTIME".equals(fieldName))
				|| ("java.lang.Class".equals(className) && "reflectionFactory".equals(fieldName)) || ("java.lang.Class".equals(className) && "useCaches".equals(fieldName)) || ("java.lang.Class".equals(className) && "initted".equals(fieldName))
				|| ("java.lang.Class$Atomic".equals(className) && "annotationTypeOffset".equals(fieldName)) || ("java.lang.Class$Atomic".equals(className) && "annotationDataOffset".equals(fieldName))
				|| ("java.lang.Class$Atomic".equals(className) && "reflectionDataOffset".equals(fieldName)) || ("java.lang.Class$Atomic".equals(className) && "unsafe".equals(fieldName)) || ("java.lang.ClassLoader".equals(className) && "nocerts".equals(fieldName))
				|| ("java.lang.ClassLoader".equals(className) && "scl".equals(fieldName)) || ("java.lang.ClassLoader".equals(className) && "sclSet".equals(fieldName)) || ("java.lang.ClassLoader$ParallelLoaders".equals(className) && "loaderTypes".equals(fieldName))
				|| ("java.lang.Double".equals(className) && "TYPE".equals(fieldName)) || ("java.lang.Long".equals(className) && "TYPE".equals(fieldName)) || ("java.lang.Long$LongCache".equals(className) && "cache".equals(fieldName))
				|| ("java.lang.Math".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.lang.Math$RandomNumberGeneratorHolder".equals(className) && "randomNumberGenerator".equals(fieldName))
				|| ("java.lang.Package".equals(className) && "pkgs".equals(fieldName)) || ("java.lang.ref.Finalizer".equals(className) && "queue".equals(fieldName)) || ("java.lang.ref.Finalizer".equals(className) && "unfinalized".equals(fieldName))
				|| ("java.lang.ref.Finalizer".equals(className) && "lock".equals(fieldName)) || ("java.lang.reflect.AccessibleObject".equals(className) && "reflectionFactory".equals(fieldName))
				|| ("java.lang.reflect.Proxy$ProxyClassFactory".equals(className) && "nextUniqueNumber".equals(fieldName)) || ("java.lang.reflect.WeakCache$CacheKey".equals(className) && "NULL_KEY".equals(fieldName))
				|| ("java.lang.reflect.WeakCache$Factory".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.lang.StringCoding".equals(className) && "decoder".equals(fieldName)) || ("java.lang.StringCoding".equals(className) && "encoder".equals(fieldName))
				|| ("java.lang.Thread".equals(className) && "threadSeqNumber".equals(fieldName)) || ("java.lang.ThreadLocal".equals(className) && "nextHashCode".equals(fieldName)) || ("java.lang.Throwable".equals(className) && "SUPPRESSED_SENTINEL".equals(fieldName))
				|| ("java.lang.Throwable".equals(className) && "UNASSIGNED_STACK".equals(fieldName)) || ("java.lang.Void".equals(className) && "TYPE".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "threadLocalStringBuilderHelper".equals(fieldName))
				|| ("java.math.BigDecimal".equals(className) && "ZERO".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "ZERO_SCALED_BY".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "TEN".equals(fieldName))
				|| ("java.math.BigDecimal".equals(className) && "BIG_TEN_POWERS_TABLE".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "BIG_TEN_POWERS_TABLE_MAX".equals(fieldName))
				|| ("java.math.BigDecimal".equals(className) && "THRESHOLDS_TABLE".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "LONG_TEN_POWERS_TABLE".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "zeroThroughTen".equals(fieldName))
				|| ("java.math.BigDecimal".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "ONE".equals(fieldName)) || ("java.math.BigDecimal".equals(className) && "LONGLONG_TEN_POWERS_TABLE".equals(fieldName))
				|| ("java.math.BigInteger".equals(className) && "digitsPerLong".equals(fieldName)) || ("java.math.BigInteger".equals(className) && "longRadix".equals(fieldName)) || ("java.math.BigInteger".equals(className) && "negConst".equals(fieldName))
				|| ("java.math.BigInteger".equals(className) && "TEN".equals(fieldName)) || ("java.math.BigInteger".equals(className) && "posConst".equals(fieldName)) || ("java.math.BigInteger".equals(className) && "ONE".equals(fieldName))
				|| ("java.math.BigInteger".equals(className) && "ZERO".equals(fieldName)) || ("java.math.BigInteger".equals(className) && "zeros".equals(fieldName)) || ("java.math.MutableBigInteger".equals(className) && "ONE".equals(fieldName))
				|| ("java.math.MutableBigInteger".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "HALF_EVEN".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "HALF_UP".equals(fieldName))
				|| ("java.math.RoundingMode".equals(className) && "UNNECESSARY".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "HALF_DOWN".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "FLOOR".equals(fieldName))
				|| ("java.math.RoundingMode".equals(className) && "CEILING".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "DOWN".equals(fieldName)) || ("java.math.RoundingMode".equals(className) && "UP".equals(fieldName))
				|| ("java.math.RoundingMode".equals(className) && "$VALUES".equals(fieldName)) || ("java.net.URI".equals(className) && "H_SCHEME".equals(fieldName)) || ("java.net.URI".equals(className) && "L_SCHEME".equals(fieldName))
				|| ("java.net.URI".equals(className) && "H_ALPHA".equals(fieldName)) || ("java.net.URI".equals(className) && "H_PATH".equals(fieldName)) || ("java.net.URI".equals(className) && "L_PATH".equals(fieldName))
				|| ("java.net.URL".equals(className) && "handlers".equals(fieldName)) || ("java.nio.Bits".equals(className) && "byteOrder".equals(fieldName)) || ("java.nio.ByteOrder".equals(className) && "BIG_ENDIAN".equals(fieldName))
				|| ("java.nio.ByteOrder".equals(className) && "LITTLE_ENDIAN".equals(fieldName)) || ("java.nio.charset.Charset".equals(className) && "defaultCharset".equals(fieldName)) || ("java.nio.charset.Charset".equals(className) && "bugLevel".equals(fieldName))
				|| ("java.nio.charset.Charset".equals(className) && "cache1".equals(fieldName)) || ("java.nio.charset.CoderResult".equals(className) && "UNDERFLOW".equals(fieldName)) || ("java.nio.charset.CoderResult".equals(className) && "OVERFLOW".equals(fieldName))
				|| ("java.nio.charset.CodingErrorAction".equals(className) && "REPORT".equals(fieldName)) || ("java.nio.charset.CodingErrorAction".equals(className) && "REPLACE".equals(fieldName)) || ("java.nio.DirectLongBufferU".equals(className) && "unsafe".equals(fieldName))
				|| ("java.security.Provider".equals(className) && "knownEngines".equals(fieldName)) || ("java.security.Provider".equals(className) && "previousKey".equals(fieldName)) || ("java.security.Security".equals(className) && "spiMap".equals(fieldName))
				|| ("java.text.DecimalFormat".equals(className) && "EmptyFieldPositionArray".equals(fieldName)) || ("java.text.DecimalFormat".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("java.text.DecimalFormat$DigitArrays".equals(className) && "DigitHundreds1000".equals(fieldName)) || ("java.text.DecimalFormat$DigitArrays".equals(className) && "DigitTens1000".equals(fieldName))
				|| ("java.text.DecimalFormat$DigitArrays".equals(className) && "DigitOnes1000".equals(fieldName)) || ("java.text.DigitList".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("java.text.DigitList$1".equals(className) && "$SwitchMap$java$math$RoundingMode".equals(fieldName)) || ("java.text.DontCareFieldPosition".equals(className) && "INSTANCE".equals(fieldName))
				|| ("java.text.NumberFormat$Field".equals(className) && "FRACTION".equals(fieldName)) || ("java.text.NumberFormat$Field".equals(className) && "INTEGER".equals(fieldName)) || ("java.text.NumberFormat$Field".equals(className) && "SIGN".equals(fieldName))
				|| ("java.text.NumberFormat$Field".equals(className) && "DECIMAL_SEPARATOR".equals(fieldName)) || ("java.util.ArrayList".equals(className) && "EMPTY_ELEMENTDATA".equals(fieldName))
				|| ("java.util.Arrays$LegacyMergeSort".equals(className) && "userRequested".equals(fieldName)) || ("java.util.BitSet".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.util.Collections".equals(className) && "EMPTY_SET".equals(fieldName))
				|| ("java.util.Collections".equals(className) && "EMPTY_MAP".equals(fieldName)) || ("java.util.Collections".equals(className) && "EMPTY_LIST".equals(fieldName)) || ("java.util.Collections$EmptyIterator".equals(className) && "EMPTY_ITERATOR".equals(fieldName))
				|| ("java.util.ComparableTimSort".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.util.concurrent.atomic.AtomicReference".equals(className) && "unsafe".equals(fieldName))
				|| ("java.util.concurrent.atomic.AtomicReference".equals(className) && "valueOffset".equals(fieldName)) || ("java.util.concurrent.atomic.AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl".equals(className) && "unsafe".equals(fieldName))
				|| ("java.util.concurrent.ConcurrentHashMap".equals(className) && "BASECOUNT".equals(fieldName)) || ("java.util.concurrent.ConcurrentHashMap".equals(className) && "ABASE".equals(fieldName))
				|| ("java.util.concurrent.ConcurrentHashMap".equals(className) && "ASHIFT".equals(fieldName)) || ("java.util.concurrent.ConcurrentHashMap".equals(className) && "U".equals(fieldName))
				|| ("java.util.concurrent.ConcurrentHashMap".equals(className) && "SIZECTL".equals(fieldName)) || ("java.util.concurrent.ConcurrentHashMap".equals(className) && "TRANSFERINDEX".equals(fieldName))
				|| ("java.util.concurrent.ConcurrentHashMap".equals(className) && "NCPU".equals(fieldName)) || ("java.util.concurrent.ConcurrentHashMap".equals(className) && "RESIZE_STAMP_SHIFT".equals(fieldName))
				|| ("java.util.concurrent.ConcurrentHashMap".equals(className) && "RESIZE_STAMP_BITS".equals(fieldName)) || ("java.util.concurrent.FutureTask".equals(className) && "UNSAFE".equals(fieldName))
				|| ("java.util.concurrent.locks.AbstractQueuedSynchronizer".equals(className) && "unsafe".equals(fieldName)) || ("java.util.concurrent.locks.AbstractQueuedSynchronizer".equals(className) && "stateOffset".equals(fieldName))
				|| ("java.util.concurrent.locks.AbstractQueuedSynchronizer".equals(className) && "tailOffset".equals(fieldName)) || ("java.util.concurrent.locks.AbstractQueuedSynchronizer".equals(className) && "waitStatusOffset".equals(fieldName))
				|| ("java.util.concurrent.locks.LockSupport".equals(className) && "UNSAFE".equals(fieldName)) || ("java.util.concurrent.locks.LockSupport".equals(className) && "parkBlockerOffset".equals(fieldName))
				|| ("java.util.Currency".equals(className) && "instances".equals(fieldName)) || ("java.util.Currency".equals(className) && "mainTable".equals(fieldName)) || ("java.util.Currency$CurrencyNameGetter".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("java.util.Currency$CurrencyNameGetter".equals(className) && "INSTANCE".equals(fieldName)) || ("java.util.Date".equals(className) && "gcal".equals(fieldName)) || ("java.util.Formatter".equals(className) && "fsPattern".equals(fieldName))
				|| ("java.util.Formatter$Flags".equals(className) && "UPPERCASE".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "ALTERNATE".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "PARENTHESES".equals(fieldName))
				|| ("java.util.Formatter$Flags".equals(className) && "GROUP".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "ZERO_PAD".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "LEADING_SPACE".equals(fieldName))
				|| ("java.util.Formatter$Flags".equals(className) && "PLUS".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "LEFT_JUSTIFY".equals(fieldName)) || ("java.util.Formatter$Flags".equals(className) && "PREVIOUS".equals(fieldName))
				|| ("java.util.Formatter$Flags".equals(className) && "NONE".equals(fieldName)) || ("java.util.HashMap$TreeNode".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.util.HashSet".equals(className) && "PRESENT".equals(fieldName))
				|| ("java.util.jar.Attributes$Name".equals(className) && "SEALED".equals(fieldName)) || ("java.util.Locale".equals(className) && "US".equals(fieldName)) || ("java.util.Locale".equals(className) && "ENGLISH".equals(fieldName))
				|| ("java.util.Locale".equals(className) && "ROOT".equals(fieldName)) || ("java.util.Locale".equals(className) && "LOCALECACHE".equals(fieldName)) || ("java.util.Locale$1".equals(className) && "$SwitchMap$java$util$Locale$Category".equals(fieldName))
				|| ("java.util.Locale$Category".equals(className) && "FORMAT".equals(fieldName)) || ("java.util.Locale$Category".equals(className) && "DISPLAY".equals(fieldName)) || ("java.util.Random".equals(className) && "seedUniquifier".equals(fieldName))
				|| ("java.util.regex.ASCII".equals(className) && "ctype".equals(fieldName)) || ("java.util.regex.Pattern".equals(className) && "accept".equals(fieldName)) || ("java.util.regex.Pattern".equals(className) && "lastAccept".equals(fieldName))
				|| ("java.util.ResourceBundle".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.util.ResourceBundle".equals(className) && "NONEXISTENT_BUNDLE".equals(fieldName))
				|| ("java.util.ResourceBundle".equals(className) && "cacheList".equals(fieldName)) || ("java.util.ResourceBundle".equals(className) && "referenceQueue".equals(fieldName))
				|| ("java.util.ResourceBundle$Control".equals(className) && "CANDIDATES_CACHE".equals(fieldName)) || ("java.util.ResourceBundle$Control".equals(className) && "FORMAT_DEFAULT".equals(fieldName))
				|| ("java.util.ResourceBundle$Control".equals(className) && "INSTANCE".equals(fieldName)) || ("java.util.ResourceBundle$RBClassLoader".equals(className) && "loader".equals(fieldName))
				|| ("java.util.ResourceBundle$RBClassLoader".equals(className) && "INSTANCE".equals(fieldName)) || ("java.util.TimeZone".equals(className) && "defaultTimeZone".equals(fieldName))
				|| ("java.util.TimSort".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("java.util.zip.Inflater".equals(className) && "defaultBuf".equals(fieldName)) || ("java.util.zip.Inflater".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.misc.FDBigInteger".equals(className) && "LONG_5_POW".equals(fieldName)) || ("sun.misc.FDBigInteger".equals(className) && "ZERO".equals(fieldName)) || ("sun.misc.FDBigInteger".equals(className) && "POW_5_CACHE".equals(fieldName))
				|| ("sun.misc.FDBigInteger".equals(className) && "SMALL_5_POW".equals(fieldName)) || ("sun.misc.FDBigInteger".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.misc.FloatingDecimal".equals(className) && "threadLocalBinaryToASCIIBuffer".equals(fieldName)) || ("sun.misc.FloatingDecimal".equals(className) && "B2AC_POSITIVE_ZERO".equals(fieldName))
				|| ("sun.misc.FloatingDecimal".equals(className) && "B2AC_POSITIVE_INFINITY".equals(fieldName)) || ("sun.misc.FloatingDecimal".equals(className) && "B2AC_NEGATIVE_INFINITY".equals(fieldName))
				|| ("sun.misc.FloatingDecimal".equals(className) && "B2AC_NOT_A_NUMBER".equals(fieldName)) || ("sun.misc.FloatingDecimal$ASCIIToBinaryBuffer".equals(className) && "TINY_10_POW".equals(fieldName))
				|| ("sun.misc.FloatingDecimal$ASCIIToBinaryBuffer".equals(className) && "SMALL_10_POW".equals(fieldName)) || ("sun.misc.FloatingDecimal$ASCIIToBinaryBuffer".equals(className) && "MAX_SMALL_TEN".equals(fieldName))
				|| ("sun.misc.FloatingDecimal$BinaryToASCIIBuffer".equals(className) && "N_5_BITS".equals(fieldName)) || ("sun.misc.FloatingDecimal$BinaryToASCIIBuffer".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.misc.PerfCounter$CoreCounters".equals(className) && "lc".equals(fieldName)) || ("sun.misc.PerfCounter$CoreCounters".equals(className) && "lct".equals(fieldName)) || ("sun.misc.PerfCounter$CoreCounters".equals(className) && "pdt".equals(fieldName))
				|| ("sun.misc.PerfCounter$CoreCounters".equals(className) && "rcbt".equals(fieldName)) || ("sun.misc.ProxyGenerator".equals(className) && "saveGeneratedFiles".equals(fieldName))
				|| ("sun.misc.ProxyGenerator".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("sun.misc.ProxyGenerator".equals(className) && "toStringMethod".equals(fieldName)) || ("sun.misc.ProxyGenerator".equals(className) && "equalsMethod".equals(fieldName))
				|| ("sun.misc.ProxyGenerator".equals(className) && "hashCodeMethod".equals(fieldName)) || ("sun.misc.ProxyGenerator$PrimitiveTypeInfo".equals(className) && "table".equals(fieldName))
				|| ("sun.misc.SharedSecrets".equals(className) && "javaLangAccess".equals(fieldName)) || ("sun.misc.Unsafe".equals(className) && "theUnsafe".equals(fieldName)) || ("sun.misc.URLClassPath".equals(className) && "DEBUG".equals(fieldName))
				|| ("sun.misc.VM".equals(className) && "allowArraySyntax".equals(fieldName)) || ("sun.misc.VM".equals(className) && "peakFinalRefCount".equals(fieldName)) || ("sun.misc.VM".equals(className) && "finalRefCount".equals(fieldName))
				|| ("sun.net.www.ParseUtil".equals(className) && "encodedInPath".equals(fieldName)) || ("sun.nio.cs.StreamEncoder".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.reflect.AccessorGenerator".equals(className) && "primitiveTypes".equals(fieldName)) || ("sun.reflect.annotation.AnnotationInvocationHandler".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.reflect.annotation.AnnotationParser".equals(className) && "EMPTY_ANNOTATION_ARRAY".equals(fieldName)) || ("sun.reflect.ClassDefiner".equals(className) && "unsafe".equals(fieldName))
				|| ("sun.reflect.generics.parser.SignatureParser".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("sun.reflect.generics.visitor.Reifier".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.reflect.MethodAccessorGenerator".equals(className) && "constructorSymnum".equals(fieldName)) || ("sun.reflect.MethodAccessorGenerator".equals(className) && "serializationConstructorSymnum".equals(fieldName))
				|| ("sun.reflect.MethodAccessorGenerator".equals(className) && "methodSymnum".equals(fieldName)) || ("sun.reflect.UnsafeFieldAccessorImpl".equals(className) && "unsafe".equals(fieldName))
				|| ("sun.security.jca.Providers".equals(className) && "providerList".equals(fieldName)) || ("sun.security.provider.ByteArrayAccess".equals(className) && "unsafe".equals(fieldName))
				|| ("sun.security.provider.ByteArrayAccess".equals(className) && "byteArrayOfs".equals(fieldName)) || ("sun.security.provider.ByteArrayAccess".equals(className) && "littleEndianUnaligned".equals(fieldName))
				|| ("sun.security.provider.DigestBase".equals(className) && "padding".equals(fieldName)) || ("sun.util.calendar.BaseCalendar".equals(className) && "$assertionsDisabled".equals(fieldName))
				|| ("sun.util.calendar.BaseCalendar".equals(className) && "ACCUMULATED_DAYS_IN_MONTH".equals(fieldName)) || ("sun.util.calendar.BaseCalendar".equals(className) && "FIXED_DATES".equals(fieldName))
				|| ("sun.util.locale.BaseLocale".equals(className) && "CACHE".equals(fieldName)) || ("sun.util.locale.provider.LocaleDataMetaInfo".equals(className) && "resourceNameToLocales".equals(fieldName))
				|| ("sun.util.locale.provider.LocaleProviderAdapter".equals(className) && "jreLocaleProviderAdapter".equals(fieldName)) || ("sun.util.locale.provider.LocaleProviderAdapter".equals(className) && "adapterCache".equals(fieldName))
				|| ("sun.util.locale.provider.LocaleProviderAdapter".equals(className) && "defaultLocaleProviderAdapter".equals(fieldName))
				|| ("sun.util.locale.provider.LocaleProviderAdapter$1".equals(className) && "$SwitchMap$sun$util$locale$provider$LocaleProviderAdapter$Type".equals(fieldName)) || ("sun.util.locale.provider.LocaleProviderAdapter$Type".equals(className) && "JRE".equals(fieldName))
				|| ("sun.util.locale.provider.LocaleResources".equals(className) && "NULLOBJECT".equals(fieldName)) || ("sun.util.locale.provider.LocaleServiceProviderPool".equals(className) && "poolOfPools".equals(fieldName))
				|| ("sun.util.resources.LocaleData$LocaleDataResourceBundleControl".equals(className) && "$assertionsDisabled".equals(fieldName)) || ("sun.util.resources.LocaleData$LocaleDataResourceBundleControl".equals(className) && "INSTANCE".equals(fieldName));
	}

	private static final LinkedHashMap<String, Object> nameToInstance = new LinkedHashMap();
	static int testCount = 1;
	public static HashMap<Integer, String> testNumToMethod = new HashMap<Integer, String>();
	public static HashMap<Integer, String> testNumToTestClass = new HashMap<>();

	public static synchronized void resetAllState() {
		testCount = 1;
		DependencyInfo.IN_CAPTURE = true;
		DependencyInfo.CURRENT_TEST_COUNT = 1;
		for (StaticField sf : sfPool) {
			sf.clearConflict();
		}
		sfPool.clear();
		for (WeakReference<DependencyInfo> i : lastGenReachable) {
			if (i.get() != null) {
				i.get().clearConflict();
			}
		}
		DependencyInfo.IN_CAPTURE = false;
	}

	public static synchronized LinkedList<StaticFieldDependency> walkAndFindDependencies(String className, String methodName) {
		DependencyInfo.IN_CAPTURE = true;
		testNumToMethod.put(testCount, methodName);
		testNumToTestClass.put(testCount, className);
		testCount++;
		// First - clean up from last generation: make sure that all static
		// field up-pointers are cleared out
		for (WeakReference<DependencyInfo> inf : lastGenReachable) {
			if (inf.get() != null) {
				inf.get().clearSFs();
				inf.get().clearConflict();
			}
		}
		lastGenReachable.clear();

		LinkedList<StaticFieldDependency> deps = new LinkedList<StaticFieldDependency>();
		for (StaticField sf : sfPool) {
			if (sf.isConflict()) {
				StaticFieldDependency dep = new StaticFieldDependency();
				dep.depGen = sf.dependsOn;
				dep.field = sf.field;
				dep.value = sf.getValue();
				deps.add(dep);
				sf.clearConflict();
			}
		}
		sfPool.clear();
		HashMap<String, StaticField> cache = new HashMap<String, StaticField>();
		for (Class<?> c : PreMain.getInstrumentation().getAllLoadedClasses()) {
			Set<Field> allFields = new HashSet<Field>();
			try {
				Field[] declaredFields = c.getDeclaredFields();
				Field[] fields = c.getFields();
				allFields.addAll(Arrays.asList(declaredFields));
				allFields.addAll(Arrays.asList(fields));
			} catch (NoClassDefFoundError e) {
				continue;
			}
			cache.clear();
			for (Field f : allFields) {
				String fieldName = getFieldFQN(f);
				// if (!ignores.contains(fieldName)) {
				if ((Modifier.isStatic(f.getModifiers())) && !((Modifier.isFinal(f.getModifiers())) && (f.getType().isPrimitive())))
					try {
						if (isBlackListedSF(f)) {
							if (f.getType().isPrimitive()) {
								try {
									Field depInfo = f.getDeclaringClass().getDeclaredField(f.getName() + "__DEPENDENCY_INFO");
									depInfo.setAccessible(true);
									DependencyInfo i = (DependencyInfo) depInfo.get(null);
									i.setIgnored(true);
								} catch (Throwable t) {
									// Maybe the field doesn't exist on this
									// versin of the JDK, so ignore
								}
							} else {
								f.setAccessible(true);
								Object obj = f.get(null);
								visitFieldForIgnore(obj);
							}
						} else if (shouldCapture(f)) {
							if (f.getName().endsWith("__DEPENDENCY_INFO")) {
								fieldName = fieldName.replace("__DEPENDENCY_INFO", "");
								if (!cache.containsKey(fieldName)) {
									cache.put(fieldName, new StaticField(f.getDeclaringClass().getDeclaredField(f.getName().replace("__DEPENDENCY_INFO", ""))));
								}
								StaticField sf = cache.get(fieldName);
								f.setAccessible(true);
								DependencyInfo inf = (DependencyInfo) f.get(null);
								if (inf != null) {
									inf.fields = new StaticField[1];
									inf.fields[0] = sf;
								}
								Field origField = f.getDeclaringClass().getDeclaredField(f.getName().replace("__DEPENDENCY_INFO", ""));
								if (origField.getType().isPrimitive())
									sfPool.add(sf);

							} else if (!f.getType().isPrimitive()) {
								if (!cache.containsKey(fieldName))
									cache.put(fieldName, new StaticField(f));
								StaticField sf = cache.get(fieldName);
								f.setAccessible(true);
								Object obj = f.get(null);
								sfPool.add(sf);
								visitField(sf, obj, false);
							}
						}

					} catch (NoClassDefFoundError e) {
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
			// }
		}

		DependencyInfo.CURRENT_TEST_COUNT++;
		DependencyInfo.IN_CAPTURE = false;
		return deps;
	}

	private static void visitFieldForIgnore(Object obj) {
		if (obj != null) {
			DependencyInfo inf = TagHelper.getOrFetchTag(obj);
			if (inf.isIgnored())
				return;
			inf.setIgnored(true);
			if (// inf.getCrawledGen() != DependencyInfo.CURRENT_TEST_COUNT &&
			!obj.getClass().isArray()) {
				inf.setCrawledGen();
				Set<Field> allFields = new HashSet<Field>();
				try {
					Field[] declaredFields = obj.getClass().getDeclaredFields();
					Field[] fields = obj.getClass().getFields();
					allFields.addAll(Arrays.asList(declaredFields));
					allFields.addAll(Arrays.asList(fields));
				} catch (NoClassDefFoundError e) {
				}
				for (Field f : allFields) {
					if (!f.getType().isPrimitive() && !Modifier.isStatic(f.getModifiers())) {
						try {
							f.setAccessible(true);
							visitFieldForIgnore(f.get(obj));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (obj.getClass().isArray() && !obj.getClass().getComponentType().isPrimitive()) {
				inf.setCrawledGen();
				Object[] ar = (Object[]) obj;
				for (Object o : ar) {
					visitFieldForIgnore(o);
				}
			}
		}
	}

	static XStream xStreamInst;
	public static boolean CAPTURE_TAINTS = false;

	private static XStream getXStreamInstance() {
		if (xStreamInst != null)
			return xStreamInst;
		// TODO - maintain same mapping for references when serializing multiple
		// roots
		xStreamInst = new XStream(new DepInfoReflectionProvider()) {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {

				return new FilteringFieldMapper(next);
			}
		};
		xStreamInst.registerConverter(new WrappedPrimitiveConverter(), XStream.PRIORITY_VERY_HIGH);
		xStreamInst.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(ReferenceByXPathMarshallingStrategy.ABSOLUTE) {
			@Override
			protected TreeMarshaller createMarshallingContext(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper) {
				return new ReferenceByXPathWithDependencysMarshaller(writer, converterLookup, mapper, ReferenceByXPathMarshallingStrategy.ABSOLUTE);
			}
		});

		// for (String ignore : ignores) {
		// int lastDot = ignore.lastIndexOf(".");
		// String clz = ignore.substring(0, lastDot);
		// String fld = ignore.substring(lastDot + 1);
		// try {
		// xStreamInst.omitField(Class.forName(clz), fld);
		// } catch (Exception ex) {
		// }
		// }
		return xStreamInst;
	}

	public static void main(String[] args) {
		HashMap<Object, Object> hm = new HashMap<Object, Object>();
		// hm.put(new LinkedList<String>(), "baz");
		hm.put("a", "baz");
		hm.put("b", "z");

		hm.put("aaa", "baz");
		System.out.println(HeapWalker.serialize(hm));
	}

	public static synchronized Element serialize(Object obj) {
		if (DependencyInfo.IN_CAPTURE)
			return null;
		// if(obj != null &&
		// obj.getClass().getName().contains("edu.columbia.cs.psl.testdepends"))
		// return null;
		try {
			DependencyInfo.IN_CAPTURE = true;
			Element root = new Element("root");
			JDomHackWriter wr = new JDomHackWriter(root);
			// getXStreamInstance().marshal(obj, new CompactWriter(sw));
			getXStreamInstance().marshal(obj, wr);

			DependencyInfo.IN_CAPTURE = false;
			return root;
		} catch (Throwable t) {
			System.err.println("Unable to serialize object!");
			t.printStackTrace();
			return null;
		}
	}

	static LinkedList<WeakReference<DependencyInfo>> lastGenReachable = new LinkedList<WeakReference<DependencyInfo>>();

	static void visitField(StaticField root, Object obj, boolean alreadyInConflict) {
		// LinkedList<StaticFieldDependency> ret = new
		// LinkedList<StaticFieldDependency>();
		if (obj != null) {
			DependencyInfo inf = TagHelper.getOrFetchTag(obj);
			if (inf.getCrawledGen() == DependencyInfo.CURRENT_TEST_COUNT) {
				lastGenReachable.add(new WeakReference<DependencyInfo>(inf));
				// return ret; //Not actually OK to bail early if we want to
				// know everything that points to everything!
			}
			// Did we already get here from this sf root this generation though?
			boolean found = false;
			if (inf.fields == null) {
				inf.fields = new StaticField[10];
			} else {
				for (StaticField sf : inf.fields)
					if (root == sf)
						found = true;
			}
			if (inf.isConflict()) {
				inf.clearConflict();
			}
			if (inf.getWriteGen() == 0) {
				inf.write();
			}
			if (found)
				return;
			boolean inserted = false;
			for (int i = 0; i < inf.fields.length && !inserted; i++) {
				if (inf.fields[i] == null) {
					inf.fields[i] = root;
					inserted = true;
				}
			}
			if (!inserted) {
				// out of space
				int k = inf.fields.length;
				StaticField[] tmp = new StaticField[inf.fields.length + 10];
				System.arraycopy(inf.fields, 0, tmp, 0, inf.fields.length);
				inf.fields = tmp;
				inf.fields[k] = root;
			}

			if (obj.getClass() == DependencyInfo.class)
				return;
			if (// inf.getCrawledGen() != DependencyInfo.CURRENT_TEST_COUNT &&
			!obj.getClass().isArray()) {
				inf.setCrawledGen();
				Set<Field> allFields = new HashSet<Field>();
				try {
					Field[] declaredFields = obj.getClass().getDeclaredFields();
					Field[] fields = obj.getClass().getFields();
					allFields.addAll(Arrays.asList(declaredFields));
					allFields.addAll(Arrays.asList(fields));
				} catch (NoClassDefFoundError e) {
				}
				for (Field f : allFields) {
					if (!f.getType().isPrimitive() && !Modifier.isStatic(f.getModifiers())) {
						try {
							f.setAccessible(true);
							visitField(root, f.get(obj), alreadyInConflict);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (obj.getClass().isArray() && !obj.getClass().getComponentType().isPrimitive()) {
				inf.setCrawledGen();
				Object[] ar = (Object[]) obj;
				for (Object o : ar) {
					visitField(root, o, alreadyInConflict);
				}
			}
		}
	}
}
