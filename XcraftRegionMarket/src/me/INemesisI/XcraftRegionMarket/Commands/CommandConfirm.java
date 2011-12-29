package me.INemesisI.XcraftRegionMarket.Commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.INemesisI.XcraftRegionMarket.Globalprice;
import me.INemesisI.XcraftRegionMarket.MarketSign;
import me.INemesisI.XcraftRegionMarket.Rent;
import me.INemesisI.XcraftRegionMarket.XcraftRegionMarket;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class CommandConfirm extends CommandHelper {

	public CommandConfirm(XcraftRegionMarket instance) {
		super(instance);
	}

	@Override
	public void execute(CommandSender sender, String Command, List<String> args) {
		this.sender = sender;
		this.player = (Player) sender;
		this.economy = plugin.getEconomy();
		this.worldguard = plugin.getWorldguard();

		if (Command.equals("confirm")) {
			MarketSign ms = getClicked().get(sender.getName());
			if (ms == null) {
				error("Du musst erst auf ein Schild klicken!");
				return;
			}
			if (!ms.getType().equals("sell") && !ms.getType().equals("rent")) return;
			Map<String, Integer> count = new HashMap<String, Integer>();
			if (ms.getType().equals("sell")) count = plugin.regionHandler.getRegionCount(player, player.getWorld(), "sold");
			else
				count = plugin.regionHandler.getRegionCount(player, player.getWorld(), "rented");
			if (ms.getOwner().equals(sender.getName())) {
				error("Du kannst dein eigenes Grundstück nicht kaufen. Nutze /rm stop um den Verkauf zu stoppen");
				return;
			}
			if (!economy.getAccount(player.getName()).hasEnough(ms.getPrice())) {
				error("Du hast nicht genug Geld!");
				return;
			}
			ProtectedRegion region = plugin.regionHandler.getRegion(ms);
			if (!plugin.regionHandler.canBuy(player.getName(), player.getWorld().getName(), ms.getType(), plugin.regionHandler.getRegion(ms), count)) {
				error("Du hast dein Limit an Regionen erreicht. Du kannst nicht noch mehr Regionen besitzen!");
				return;
			}
			// set money
			economy.getAccount(ms.getOwner()).add(ms.getPrice());
			economy.getAccount(player.getName()).subtract(ms.getPrice());
			// set regionowner
			plugin.regionHandler.setPlayer(region, player.getName());
			plugin.regionHandler.addGroup(region, "xrm");

			if (ms.getType().equals("sell") && player.hasPermission("XcraftRegionMarket.Buy")) {
				// inform the players
				Player seller = plugin.getServer().getPlayer(ms.getOwner());
				if (seller != null) seller.sendMessage(plugin.getName() + player.getName() + " hat dein Grundstück " + ms.getRegion() + " für " + economy.format(ms.getPrice()) + " gekauft!");
				reply("Du hast das Grundstück " + ms.getRegion() + " von " + ms.getOwner() + " für " + ms.getPrice() + " gekauft!");
				// set sign text
				ms.setType("sold");
				ms.setOwner(player.getName());
				plugin.marketHandler.update(ms);
				// set group
				plugin.groupHandler.setPermGroup(player.getName(), ms.getType(), count.get("global"));
				// save region
				plugin.regionHandler.saveRegion(ms.getBlock().getWorld());
			}
			if (ms.getType().equals("rent") && player.hasPermission("XcraftRegionMarket.Rent")) {
				// set rentsign
				Rent rent = new Rent(ms.getBlock(), ms.getRegion(), player.getName(), ms.getPrice(), ms.getIntervall());
				for (Globalprice gp : plugin.marketHandler.getGlobalPrices()) {
					if (gp.removeSign(ms) == true) gp.addRent(rent);
				}
				rent.setRenter(ms.getOwner());
				plugin.rentHandler.add(rent);
				// inform the players
				Player seller = plugin.getServer().getPlayer(rent.getRenter());
				if (seller != null) seller.sendMessage(plugin.getName() + ms.getOwner() + " hat dein Grundstück " + ms.getRegion() + " für " + economy.format(ms.getPrice()) + " gemietet!");
				reply("Du hast das Grundstück " + ms.getRegion() + " von " + rent.getRenter() + " für " + economy.format(ms.getPrice()) + " pro " + ms.getIntervall() + " gemietet!");
				// set sign text
				ms.setType("rented");
				ms.setOwner(player.getName());
				plugin.marketHandler.update(ms);
				plugin.marketHandler.remove(ms);
				// set group
				plugin.groupHandler.setPermGroup(player.getName(), ms.getType(), count.get("global"));
				// save region
				plugin.regionHandler.saveRegion(ms.getBlock().getWorld());
			}
		}
	}

}
