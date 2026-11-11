#include <iostream>
#include <fstream>
#include <filesystem>
#include <string>
#include <algorithm>
#include <cctype>
namespace fs = std::filesystem;

// ---------- Global base folder ----------
std::string BASE = ""; // = ProjectName

std::string ask(const std::string& q, const std::string& def) {
    std::cout << q << " [" << def << "]: ";
    std::string s;
    std::getline(std::cin, s);
    if (s.empty()) return def;
    s.erase(0, s.find_first_not_of(" \t\r\n"));
    if (!s.empty()) s.erase(s.find_last_not_of(" \t\r\n") + 1);
    if (s.empty()) return def;
    return s;
}

bool askYesNo(const std::string& q, bool def) {
    std::cout << q << (def ? " [Y/n]: " : " [y/N]: ");
    std::string s; std::getline(std::cin, s);
    if (s.empty()) return def;
    return std::tolower(s[0]) == 'y';
}

std::string toFolder(const std::string& pkg) {
    std::string r = pkg;
    std::replace(r.begin(), r.end(), '.', '/');
    return r;
}

std::string toLowerNoSpace(std::string s) {
    s.erase(std::remove(s.begin(), s.end(), ' '), s.end());
    std::transform(s.begin(), s.end(), s.begin(), ::tolower);
    return s;
}

// Project folder name: "My App" -> "My_App", remove bad chars
std::string sanitizeFolder(std::string s) {
    for (char &c : s) {
        if (c == ' ') c = '_';
        if (!(std::isalnum((unsigned char)c) || c == '_' || c == '-' || c == '.')) c = '_';
    }
    if (s.empty()) s = "MyApp";
    return s;
}

bool validPackage(const std::string& p) {
    if (p.empty() || p.find('.') == std::string::npos) return false;
    for (char c : p) if (!(std::isalnum((unsigned char)c) || c == '.' || c == '_')) return false;
    return true;
}

void mkDir(const std::string& p) {
    if (p.empty()) return;
    std::error_code ec;
    // p is relative like "src/res" -> prepend BASE
    fs::path full = BASE.empty() ? fs::path(p) : fs::path(BASE) / p;
    fs::create_directories(full, ec);
}

// ALL files go inside BASE / path
void writeFile(const std::string& path, const std::string& content) {
    fs::path full = BASE.empty() ? fs::path(path) : fs::path(BASE) / path;
    std::error_code ec;
    auto parent = full.parent_path();
    if (!parent.empty()) {
        fs::create_directories(parent, ec);
    }
    std::ofstream f(full);
    if (!f) {
        std::cout << "  [!] cannot write " << full.string() << std::endl;
        return;
    }
    f << content;
    std::cout << "  created: " << full.string() << std::endl;
}

int main() {
    std::cout << "=============================\n";
    std::cout << "  Noanyid Android Template\n";
    std::cout << "  Non-AndroidX - Built-in libs\n";
    std::cout << "=============================\n\n";

    // ---------- 1. PROJECT ----------
    std::cout << "--- PROJECT ---\n";
    std::string projectName = ask("Project Name", "MyApp");
    std::string appName = ask("App Name (shown on phone)", projectName);
    std::string appLabel = ask("Activity Label / Title", appName);

    std::string packageName;
    while (true) {
        packageName = ask("Package Name", "com.example." + toLowerNoSpace(projectName));
        if (validPackage(packageName)) break;
        std::cout << "  [!] Invalid package. Example: com.mycompany.myapp\n";
    }

    std::string mainActivity = ask("Main Activity Name (without .java)", "MainActivity");
    std::string secondActivity = ask("Second Activity Name (empty = skip)", "SecondActivity");
    bool wantSecond = !secondActivity.empty() && secondActivity != "none" && secondActivity != "-";

    // ---------- 2. SDK ----------
    std::cout << "\n--- SDK ---\n";
    std::string compileSdk = ask("compileSdkVersion", "34");
    std::string compileCodename = ask("compileSdkVersionCodename", "14");
    std::string minSdk = ask("minSdkVersion", "28");
    std::string targetSdk = ask("targetSdkVersion", "34");
    std::string platformBuildCode = ask("platformBuildVersionCode", compileSdk);
    std::string platformBuildName = ask("platformBuildVersionName", compileCodename);

    // ---------- 3. APP CONFIG ----------
    std::cout << "\n--- APP CONFIG ---\n";
    bool allowBackup = askYesNo("allowBackup?", true);
    bool debuggable = askYesNo("debuggable?", true);
    bool supportsRtl = askYesNo("supportsRtl?", true);
    bool resizeable = askYesNo("resizeableActivity?", true);
    std::string themeName = ask("Theme name in styles.xml", "AppTheme");
    std::string loginTheme = ask("Login Activity theme", "login");

    std::string primary = ask("Primary Color (#RRGGBB)", "#6200EE");
    std::string primaryDark = ask("Primary Dark Color", "#3700B3");
    std::string accent = ask("Accent Color", "#03DAC5");

    // ---------- 4. BUILD ----------
    std::cout << "\n--- BUILD (build.noanyid) ---\n";
    std::string apkOut = ask("APK Output path", "build/realeses/unsigned.apk");
    bool buildStyle = askYesNo("build_style? (false = default)", false);
    bool wantAssets = askYesNo("Create assets/ folder?", true);
    bool wantLibs = askYesNo("Create libs/ folder?", true);
    bool wantAar = askYesNo("Create aar/ folder?", true);
    bool wantNight = askYesNo("Create values-night (dark mode)?", true);

    // ---------- BASE FOLDER = ProjectName ----------
    BASE = sanitizeFolder(projectName);

    std::cout << "\n--- SUMMARY ---\n";
    std::cout << "Project Folder : ./" << BASE << "/\n";
    std::cout << "Project : " << projectName << "\n";
    std::cout << "Package : " << packageName << "\n";
    std::cout << "App     : " << appName << "\n";
    std::cout << "Main    : " << mainActivity << "\n";
    std::cout << "SDK     : compile=" << compileSdk << " min=" << minSdk << " target=" << targetSdk << "\n";

    if (!askYesNo("\nGenerate now?", true)) {
        std::cout << "Cancelled.\n";
        return 0;
    }

    std::cout << "\nGenerating in: " << (fs::current_path() / BASE).string() << "\n";
    std::string pkgFolder = toFolder(packageName);
    std::string javaBase = "src/java/" + pkgFolder + "/";

    // ============ build.noanyid ============
    std::string buildNoanyid =
        "print \"compile yyy\".\n\n"
        "load_use_plugin (\"Noanyid-android-plugin\").\n"
        "print (\"Hello\").\n"
        "build (project \"build\").\n\n"
        "manifest (project \"AndroidManifest.xml\").\n"
        "res (project \"src/res\").\n"
        "setApkOut (project \"" + apkOut + "\").\n"
        "assets (project \"assets\").\n"
        "genarated (project \"build/genarated\").\n"
        "flat (project \"build/flats\").\n\n"
        "aar_model_folder (project \"aar\").\n\n"
        "build_style " + std::string(buildStyle ? "true" : "false") + ".\n"
        "add_jar_folder (project \"libs\").\n"
        "add_java_folder (project \"src/java\").\n\n"
        "java.\ndexing.\nprint (\"Hi\").\n";
    writeFile("build.noanyid", buildNoanyid);

    // ============ AndroidManifest.xml ============
    std::string manifest =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
        "    android:compileSdkVersion=\"" + compileSdk + "\"\n"
        "    android:compileSdkVersionCodename=\"" + compileCodename + "\"\n"
        "    package=\"" + packageName + "\"\n"
        "    platformBuildVersionCode=\"" + platformBuildCode + "\"\n"
        "    platformBuildVersionName=\"" + platformBuildName + "\">\n"
        "    <uses-sdk\n"
        "        android:minSdkVersion=\"" + minSdk + "\"\n"
        "        android:targetSdkVersion=\"" + targetSdk + "\" />\n"
        "    <application\n"
        "        android:label=\"@string/appName\"\n"
        "        android:icon=\"@drawable/logo\"\n"
        "        android:allowBackup=\"" + std::string(allowBackup ? "true" : "false") + "\"\n"
        "        android:debuggable=\"" + std::string(debuggable ? "true" : "false") + "\"\n"
        "        android:supportsRtl=\"" + std::string(supportsRtl ? "true" : "false") + "\"\n"
        "        android:resizeableActivity=\"" + std::string(resizeable ? "true" : "false") + "\"\n"
        "        android:theme=\"@style/" + themeName + "\">\n"
        "        <activity\n"
        "            android:name=\"." + mainActivity + "\"\n"
        "            android:label=\"@string/title\"\n"
        "            android:theme=\"@style/" + loginTheme + "\"\n"
        "            android:exported=\"true\">\n"
        "            <intent-filter>\n"
        "                <action android:name=\"android.intent.action.MAIN\" />\n"
        "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
        "            </intent-filter>\n"
        "        </activity>\n";
    if (wantSecond) manifest += "        <activity android:name=\"." + secondActivity + "\" android:exported=\"false\" />\n";
    manifest += "    </application>\n</manifest>\n";
    writeFile("AndroidManifest.xml", manifest);

    // ============ Java ============
    std::string mainJava =
        "package " + packageName + ";\n\n"
        "import android.app.Activity;\n"
        "import android.content.Intent;\n"
        "import android.os.Bundle;\n"
        "import android.view.View;\n"
        "import android.widget.Button;\n"
        "import android.widget.TextView;\n"
        "import android.widget.Toast;\n\n"
        "public class " + mainActivity + " extends Activity {\n"
        "    private TextView tvHello;\n"
        "    private Button btnNext;\n\n"
        "    @Override\n"
        "    protected void onCreate(Bundle savedInstanceState) {\n"
        "        super.onCreate(savedInstanceState);\n"
        "        setContentView(R.layout.activity_main);\n\n"
        "        tvHello = (TextView) findViewById(R.id.tvHello);\n"
        "        btnNext = (Button) findViewById(R.id.btnNext);\n"
        "        tvHello.setText(getString(R.string.hello));\n\n";
    if (wantSecond) {
        mainJava +=
            "        btnNext.setOnClickListener(new View.OnClickListener() {\n"
            "            @Override public void onClick(View v) {\n"
            "                Toast.makeText(" + mainActivity + ".this, getString(R.string.going_next), Toast.LENGTH_SHORT).show();\n"
            "                startActivity(new Intent(" + mainActivity + ".this, " + secondActivity + ".class));\n"
            "            }\n"
            "        });\n";
    } else {
        mainJava +=
            "        btnNext.setOnClickListener(new View.OnClickListener() {\n"
            "            @Override public void onClick(View v) {\n"
            "                Toast.makeText(" + mainActivity + ".this, getString(R.string.hello), Toast.LENGTH_SHORT).show();\n"
            "            }\n"
            "        });\n";
    }
    mainJava += "    }\n}\n";
    writeFile(javaBase + mainActivity + ".java", mainJava);

    if (wantSecond) {
        writeFile(javaBase + secondActivity + ".java",
            "package " + packageName + ";\n\n"
            "import android.app.Activity;\n"
            "import android.os.Bundle;\n"
            "import android.widget.TextView;\n\n"
            "public class " + secondActivity + " extends Activity {\n"
            "    @Override protected void onCreate(Bundle savedInstanceState) {\n"
            "        super.onCreate(savedInstanceState);\n"
            "        setContentView(R.layout.activity_second);\n"
            "        TextView tv = (TextView) findViewById(R.id.tvSecond);\n"
            "        tv.setText(getString(R.string.second_screen));\n"
            "    }\n}\n");
    }

    // ============ Layouts ============
    writeFile("src/res/layout/activity_main.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp"
    android:background="@color/background">
    <ImageView android:layout_width="120dp" android:layout_height="120dp"
        android:src="@drawable/logo" android:contentDescription="@string/appName" />
    <TextView android:id="@+id/tvHello" android:layout_width="wrap_content"
        android:layout_height="wrap_content" android:layout_marginTop="24dp"
        android:text="@string/hello" android:textSize="24sp" android:textColor="@color/textPrimary" />
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_marginTop="8dp" android:text="@string/welcome_desc"
        android:textSize="16sp" android:textColor="@color/textSecondary" />
    <Button android:id="@+id/btnNext" android:layout_width="match_parent"
        android:layout_height="wrap_content" android:layout_marginTop="32dp"
        android:text="@string/next" android:background="@drawable/btn_rounded"
        android:textColor="@android:color/white" />
    <include layout="@layout/item_footer" />
</LinearLayout>
)");

    if (wantSecond) {
        writeFile("src/res/layout/activity_second.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:gravity="center" android:padding="24dp">
    <TextView android:id="@+id/tvSecond" android:layout_width="wrap_content"
        android:layout_height="wrap_content" android:text="@string/second_screen" android:textSize="22sp" />
</LinearLayout>
)");
    }

    writeFile("src/res/layout/item_footer.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:layout_marginTop="24dp" android:text="@string/footer"
    android:textSize="12sp" android:textColor="@color/textSecondary" />
)");

    // ============ Values ============
    writeFile("src/res/values/strings.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
        "    <string name=\"appName\">" + appName + "</string>\n"
        "    <string name=\"title\">" + appLabel + "</string>\n"
        "    <string name=\"hello\">Hello, World!</string>\n"
        "    <string name=\"welcome_desc\">Welcome to " + appName + " - NoAndroidX template.</string>\n"
        "    <string name=\"next\">Next Screen</string>\n"
        "    <string name=\"going_next\">Going to next screen...</string>\n"
        "    <string name=\"second_screen\">This is Second Screen</string>\n"
        "    <string name=\"footer\">Built with Noanyid - No AndroidX</string>\n"
        "</resources>\n");

    writeFile("src/res/values/colors.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
        "    <color name=\"primary\">" + primary + "</color>\n"
        "    <color name=\"primaryDark\">" + primaryDark + "</color>\n"
        "    <color name=\"accent\">" + accent + "</color>\n"
        "    <color name=\"background\">#FFFFFF</color>\n"
        "    <color name=\"textPrimary\">#121212</color>\n"
        "    <color name=\"textSecondary\">#757575</color>\n"
        "    <color name=\"white\">#FFFFFF</color>\n"
        "</resources>\n");

    writeFile("src/res/values/dimens.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="padding_small">8dp</dimen>
    <dimen name="padding_medium">16dp</dimen>
    <dimen name="padding_large">24dp</dimen>
    <dimen name="text_small">12sp</dimen>
    <dimen name="text_medium">16sp</dimen>
    <dimen name="text_large">24sp</dimen>
</resources>
)");

    writeFile("src/res/values/styles.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
        "    <style name=\"" + themeName + "\" parent=\"@android:style/Theme.Material.Light.NoActionBar\">\n"
        "        <item name=\"android:colorPrimary\">@color/primary</item>\n"
        "        <item name=\"android:colorPrimaryDark\">@color/primaryDark</item>\n"
        "        <item name=\"android:colorAccent\">@color/accent</item>\n"
        "        <item name=\"android:windowBackground\">@color/background</item>\n"
        "    </style>\n"
        "    <style name=\"" + loginTheme + "\" parent=\"" + themeName + "\">\n"
        "        <item name=\"android:windowBackground\">@color/background</item>\n"
        "    </style>\n"
        "</resources>\n");

    writeFile("src/res/values/themes.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
        "    <style name=\"Theme." + projectName + "\" parent=\"@android:style/Theme.Material.Light.NoActionBar\">\n"
        "        <item name=\"android:colorPrimary\">@color/primary</item>\n"
        "        <item name=\"android:colorPrimaryDark\">@color/primaryDark</item>\n"
        "        <item name=\"android:colorAccent\">@color/accent</item>\n"
        "    </style>\n"
        "</resources>\n");

    // ============ Drawables ============
    writeFile("src/res/drawable/logo.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp" android:height="120dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/primary" android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z" />
    <path android:fillColor="@color/white" android:pathData="M12,7l-5,5h3v5h4v-5h3z" />
</vector>
)");
    writeFile("src/res/drawable/ic_home.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z" />
</vector>
)");
    writeFile("src/res/drawable/ic_settings.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94 0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.82,11.69 4.82,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6 3.6,1.62 3.6,3.6 -1.62,3.6 -3.6,3.6z" />
</vector>
)");
    writeFile("src/res/drawable/btn_rounded.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="12dp" />
    <solid android:color="@color/primary" />
</shape>
)");

    // ============ Folders (all inside BASE) ============
    if (wantAssets) mkDir("assets");
    if (wantLibs) mkDir("libs");
    if (wantAar) mkDir("aar");
    mkDir("build/realeses");
    mkDir("build/genarated");
    mkDir("build/flats");
    mkDir("src/res/drawable-hdpi");
    mkDir("src/res/drawable-xhdpi");
    if (wantNight) {
        writeFile("src/res/values-night/colors.xml", R"(<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="background">#121212</color>
    <color name="textPrimary">#FFFFFF</color>
    <color name="textSecondary">#B0B0B0</color>
</resources>
)");
    }
    if (wantAssets) writeFile("assets/readme.txt", "Put your assets here\n");

    std::cout << "\nDone! Project '" << projectName << "' created in ./" << BASE << "/\n";
    return 0;
}