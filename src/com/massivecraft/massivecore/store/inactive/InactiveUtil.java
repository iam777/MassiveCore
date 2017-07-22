package com.massivecraft.massivecore.store.inactive;

import com.massivecraft.massivecore.event.EventMassiveCorePlayercleanToleranceMillis;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.store.SenderColl;
import com.massivecraft.massivecore.store.SenderEntity;
import com.massivecraft.massivecore.util.TimeUnit;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public class InactiveUtil
{
	// -------------------------------------------- //
	// WHAT DO WE HANDLE?
	// -------------------------------------------- //
	
	// The standard used for non-crucial data that can easily be replaced.
	public static final long PLAYERCLEAN_TOLERANCE_MILLIS_STANDARD = 3 * TimeUnit.MILLIS_PER_MONTH;
	
	// The standard for important information that can not easily be replaced.
	public static final long PLAYERCLEAN_TOLERANCE_MILLIS_IMPORTANT = 18 * TimeUnit.MILLIS_PER_MONTH;
	
	// -------------------------------------------- //
	// LOGIC
	// -------------------------------------------- //
	
	public static void considerRemoveInactive(final SenderColl<? extends SenderEntity<?>> coll, final Iterable<CommandSender> recipients)
	{
		considerRemoveInactive(System.currentTimeMillis(), coll, recipients);
	}
	
	public static void considerRemoveInactive(long now, final SenderColl<? extends SenderEntity<?>> coll, final Iterable<CommandSender> recipients)
	{
		final long playercleanToleranceMillis = coll.getPlayercleanToleranceMillis();
		if (playercleanToleranceMillis <= 0)
		{
			for (CommandSender recipient : recipients)
			{
				MixinMessage.get().msgOne(recipient, "<h>%s<b> does not support player cleaning.", coll.getName());
			}
			return;
		}
		
		final long start = System.currentTimeMillis();
		int count = 0;
		
		final Collection<? extends SenderEntity<?>> senderEntitiesOffline = coll.getAllOffline();
		
		// For each offline player ...
		for (SenderEntity entity : senderEntitiesOffline)
		{
			// ... see if they should be removed.
			boolean result = considerRemoveInactive(now, entity, recipients);
			if (result) count++;
		}
		
		long time = System.currentTimeMillis() - start;
		int current = coll.getIds().size();
		int total = current + count;
		double percentage = (((double) count) / total) * 100D;
		for (CommandSender recipient : recipients)
		{
			MixinMessage.get().msgOne(recipient, "<i>Removed <h>%d<i>/<h>%d (%.2f%%) <i>players from <h>%s <i>took <v>%dms<i>.", count, total, percentage, coll.getName(), time);
		}
	}
	
	public static boolean considerRemoveInactive(long now, SenderEntity<?> entity, Iterable<CommandSender> recipients)
	{
		if (entity.detached()) return false;
		
		// Consider
		if (isActive(now, entity)) return false;
		
		//String message = Txt.parse("<i>Player <h>%s<i> with id %s was removed due to inactivity.", entity.getName(), entity.getId());
		
		// Apply
		entity.detach();
		
		return true;
	}
	
	public static boolean isActive(long now, SenderEntity entity)
	{
		EventMassiveCorePlayercleanToleranceMillis event = new EventMassiveCorePlayercleanToleranceMillis(now, entity);
		event.run();
		return event.shouldBeRemoved();
	}
	
	public static long getLastActivity(SenderEntity<?> entity)
	{
		if (entity instanceof Inactive) return ((Inactive) entity).getLastActivityMillis();
		
		// NOTE: This is the default implementation used by most MassiveX plugins.
		// Public plugins such as Factions should however store this information themself
		// because the Bukkit API might be unreliable.
		Long ret = entity.getLastPlayed();
		if (ret == null) ret = System.currentTimeMillis();
		return ret;
	}
	
}
