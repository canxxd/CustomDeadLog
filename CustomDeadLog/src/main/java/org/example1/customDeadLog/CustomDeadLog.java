package org.example1.customDeadLog;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomDeadLog extends JavaPlugin implements Listener {

    private File dataDosyasi;
    private final Object kilit = new Object();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getConfig().getBoolean("aktif", true)) {
            getLogger().info("DeadLog devre dışı (aktif: false).");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        dataDosyasi = new File(getDataFolder(), "data.yml");
        if (!dataDosyasi.exists()) {
            try {
                dataDosyasi.getParentFile().mkdirs();
                dataDosyasi.createNewFile();
            } catch (IOException e) {
                getLogger().severe("data.yml oluşturulamadı");
                e.printStackTrace();
            }
        }

        getLogger().info("CustomDeadLog aktif Kayıtlar data.yml dosyasına yazılıyor");
    }

    @EventHandler
    public void oyuncuOldu(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("aktif", true)) return;

        Player oyuncu = event.getEntity();
        String isim = oyuncu.getName();
        String tarih = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        Map<String, Object> olumKaydi = new LinkedHashMap<>();
        olumKaydi.put("olum-zamani", tarih);

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ItemStack item : oyuncu.getInventory().getContents()) {
            if (item == null) continue;

            Map<String, Object> itemData = new LinkedHashMap<>();
            itemData.put("tur", item.getType().toString());
            itemData.put("adet", item.getAmount());

            Map<String, Integer> buyuler = new LinkedHashMap<>();
            for (Map.Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet()) {
                buyuler.put(ench.getKey().getKey().getKey(), ench.getValue());
            }

            if (!buyuler.isEmpty()) {
                itemData.put("buyuler", buyuler);
            }

            itemList.add(itemData);
        }

        olumKaydi.put("envanter", itemList);

        synchronized (kilit) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataDosyasi);

            List<Map<String, Object>> mevcut = (List<Map<String, Object>>) yml.getList(isim);
            if (mevcut == null) mevcut = new ArrayList<>();

            mevcut.add(olumKaydi);
            yml.set(isim, mevcut);

            try {
                yml.save(dataDosyasi);
            } catch (IOException e) {
                getLogger().warning("data.yml yazılamadı");
                e.printStackTrace();
            }
        }
    }
}
