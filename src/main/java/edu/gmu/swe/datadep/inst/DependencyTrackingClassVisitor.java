package edu.gmu.swe.datadep.inst;

import java.util.LinkedList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.FieldNode;

import edu.gmu.swe.datadep.DependencyInfo;
import edu.gmu.swe.datadep.DependencyInstrumented;
import edu.gmu.swe.datadep.Instrumenter;

public class DependencyTrackingClassVisitor extends ClassVisitor {
	boolean skipFrames = false;

	public DependencyTrackingClassVisitor(ClassVisitor _cv, boolean skipFrames) {
		super(Opcodes.ASM5, _cv);
		this.skipFrames = skipFrames;
	}

	String className;
	boolean isClass = false;

	private boolean patchLDCClass;
	private boolean addTaintField = true;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// make sure class is not private
		access = access & ~Opcodes.ACC_PRIVATE;
		access = access | Opcodes.ACC_PUBLIC;
		this.isClass = (access & Opcodes.ACC_INTERFACE) == 0;
		this.className = name;
		this.patchLDCClass = (version & 0xFFFF) < Opcodes.V1_5;
		if (!superName.equals("java/lang/Object") && !Instrumenter.isIgnoredClass(superName)) {
			addTaintField = false;
		}
		// Add interface
		if (!Instrumenter.isIgnoredClass(name) && isClass && (access & Opcodes.ACC_ENUM) == 0) {
			String[] iface = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, iface, 0, interfaces.length);
			iface[interfaces.length] = Type.getInternalName(DependencyInstrumented.class);
			interfaces = iface;
			if (signature != null)
				signature = signature + Type.getDescriptor(DependencyInstrumented.class);
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	LinkedList<FieldNode> moreFields = new LinkedList<FieldNode>();

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

		Type t = Type.getType(desc);
		if ((access & Opcodes.ACC_STATIC) != 0 || (t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT))
			moreFields.add(new FieldNode(access, name + "__DEPENDENCY_INFO", Type.getDescriptor(DependencyInfo.class), null, null));
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		AnalyzerAdapter an = null;
		if (!skipFrames) {
			an = new AnalyzerAdapter(className, access, name, desc, mv);
			mv = an;
		}
		RWTrackingMethodVisitor rtmv = new RWTrackingMethodVisitor(mv, patchLDCClass, className, access, name, desc);
		mv = rtmv;
		if (!skipFrames) {
			rtmv.setAnalyzer(an);
		}
		LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, mv);
		rtmv.setLVS(lvs);
		return lvs;
	}

	@Override
	public void visitEnd() {
		for (FieldNode fn : moreFields) {
			fn.accept(cv);
		}
		if (isClass) {

			// Add field to store dep info
			if (addTaintField)
				super.visitField(Opcodes.ACC_PUBLIC, "__DEPENDENCY_INFO", Type.getDescriptor(DependencyInfo.class), null, null);
			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "getDEPENDENCY_INFO", "()" + Type.getDescriptor(DependencyInfo.class), null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitFieldInsn(Opcodes.GETFIELD, className, "__DEPENDENCY_INFO", Type.getDescriptor(DependencyInfo.class));
			mv.visitInsn(Opcodes.DUP);
			Label ok = new Label();
			mv.visitJumpInsn(Opcodes.IFNONNULL, ok);
			mv.visitInsn(Opcodes.POP);

			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(DependencyInfo.class));
			mv.visitInsn(Opcodes.DUP_X1);
			mv.visitInsn(Opcodes.DUP);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(DependencyInfo.class), "<init>", "()V", false);
			mv.visitFieldInsn(Opcodes.PUTFIELD, className, "__DEPENDENCY_INFO", Type.getDescriptor(DependencyInfo.class));
			mv.visitLabel(ok);
			mv.visitFrame(Opcodes.F_FULL, 1, new Object[] { className }, 1, new Object[] { Type.getInternalName(DependencyInfo.class) });
			mv.visitInsn(Opcodes.ARETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();

			mv = super.visitMethod(Opcodes.ACC_PUBLIC, "__initPrimDepInfo", "()V", null, null);
			mv.visitCode();
			for (FieldNode fn : moreFields) {
				if ((fn.access & Opcodes.ACC_STATIC) == 0) {
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(DependencyInfo.class));
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(DependencyInfo.class), "<init>", "()V", false);
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(DependencyInfo.class), "write", "()V", false);
					mv.visitFieldInsn(Opcodes.PUTFIELD, className, fn.name, Type.getDescriptor(DependencyInfo.class));
				}
			}
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}
		super.visitEnd();
	}
}
