package bjohnson.apireplay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FileHandler {

//    public static File ChooseFolder(JComponent parent) {
//        Path jsonFolder = Path.of("./");
//        JFileChooser folderChooser;
//        folderChooser = new JFileChooser();
//        folderChooser.setCurrentDirectory(jsonFolder.toFile());
//        folderChooser.setDialogTitle("Select the data folder");
//        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        folderChooser.setAcceptAllFileFilterUsed(false);
//        //
//        if (folderChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
//            return folderChooser.getSelectedFile();
//        }
//        return null;
//    }
    public static List<Map<String,Object>> LoadArrayJson(Path file) throws IOException {
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(file);
        return gson.fromJson(reader, new TypeToken<List<Map<String,Object>>>(){}.getType());
    }
}
