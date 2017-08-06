package local.linux_2g64.qw.trebuchetalphabeticindexfix;

import android.content.Context;
import android.icu.text.AlphabeticIndex;

import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

@SuppressWarnings("unused")
public class Main implements IXposedHookLoadPackage {
    @SuppressWarnings("WeakerAccess")
    private static class MyAlphabeticIndex {
        private AlphabeticIndex<Object> index;

        MyAlphabeticIndex(Locale locale) {
            index = new AlphabeticIndex<>(locale);
        }

        public MyAlphabeticIndex addLabels(Locale locale) {
            index.addLabels(locale);
            return this;
        }

        public MyAlphabeticIndex setMaxLabelCount(int maxLabelCount) {
            index.setMaxLabelCount(maxLabelCount);
            return this;
        }

        public int getBucketIndex(String s) {
            return index.getBucketIndex(s);
        }

        public String getBucketLabel(int i) {
            return index.getBucketLabels().get(i);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.launcher3") &&
                !lpparam.packageName.equals("com.cyanogenmod.trebuchet"))
            return;
        XposedBridge.log(lpparam.packageName);
        findAndHookConstructor("com.android.launcher3.compat.AlphabeticIndexCompat",
                lpparam.classLoader, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (getBooleanField(param.thisObject, "mHasValidAlphabeticIndex"))
                            return;
                        Context context = (Context) param.args[0];
                        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
                        MyAlphabeticIndex index = new MyAlphabeticIndex(locale);
                        setObjectField(param.thisObject, "mAlphabeticIndex", index);
                        setObjectField(param.thisObject, "mAddLabelsMethod",
                                MyAlphabeticIndex.class.getMethod("addLabels", Locale.class));
                        setObjectField(param.thisObject, "mSetMaxLabelCountMethod",
                                MyAlphabeticIndex.class.getMethod("setMaxLabelCount", int.class));
                        setObjectField(param.thisObject, "mGetBucketIndexMethod",
                                MyAlphabeticIndex.class.getMethod("getBucketIndex", String.class));
                        setObjectField(param.thisObject, "mGetBucketLabelMethod",
                                MyAlphabeticIndex.class.getMethod("getBucketLabel", int.class));
                        try {
                            if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage()))
                                index.addLabels(Locale.ENGLISH);
                        } catch (Exception e) {
                            XposedBridge.log(e);
                        }
                        setObjectField(param.thisObject, "mDefaultMiscLabel",
                                locale.getLanguage().equals(Locale.JAPANESE.getLanguage()) ?
                                        "\u4ed6" : "\u2219");
                        setBooleanField(param.thisObject, "mHasValidAlphabeticIndex", true);
                    }
                });
    }
}
