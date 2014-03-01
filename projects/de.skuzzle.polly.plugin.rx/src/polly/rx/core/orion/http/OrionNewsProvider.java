package polly.rx.core.orion.http;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polly.rx.MSG;
import polly.rx.core.TrainManagerV2;
import polly.rx.core.TrainingEvent;
import polly.rx.core.TrainingListener;
import polly.rx.core.orion.FleetEvent;
import polly.rx.core.orion.FleetListener;
import polly.rx.core.orion.FleetTracker;
import polly.rx.core.orion.PortalEvent;
import polly.rx.core.orion.PortalListener;
import polly.rx.core.orion.PortalUpdater;
import polly.rx.core.orion.http.NewsEntry.NewsType;
import polly.rx.core.orion.model.Fleet;
import polly.rx.core.orion.model.Portal;
import polly.rx.core.orion.model.json.OrionJsonAdapter;
import de.skuzzle.polly.http.api.HttpEvent;
import de.skuzzle.polly.http.api.HttpException;
import de.skuzzle.polly.http.api.answers.HttpAnswer;
import de.skuzzle.polly.http.api.answers.HttpAnswers;
import de.skuzzle.polly.http.api.handler.HttpEventHandler;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.User;
import de.skuzzle.polly.sdk.httpv2.GsonHttpAnswer;
import de.skuzzle.polly.sdk.httpv2.SuccessResult;
import de.skuzzle.polly.sdk.time.Time;

public class OrionNewsProvider implements HttpEventHandler, FleetListener, PortalListener, 
        TrainingListener {

    public final static String NEWS_URL = "/api/orion/json/news"; //$NON-NLS-1$
            
    private final static int MAX_NEWS = 100;
    private final Deque<NewsEntry> entries;
    private final Map<String, Deque<NewsEntry>> forVenads;

    private final MyPolly myPolly;
    

    
    public OrionNewsProvider(MyPolly myPolly, FleetTracker fleetTracker, 
            PortalUpdater portalUpdater, TrainManagerV2 trainManager) {
        this.myPolly = myPolly;
        this.entries = new ArrayDeque<>();
        this.forVenads = new HashMap<>();
        fleetTracker.addFleetListener(this);
        portalUpdater.addPortalListener(this);
        trainManager.addTrainListener(this);
    }
    
    
    
    private void addNews(NewsEntry e) {
        synchronized (this.entries) {
            this.addNews(this.entries, e);
        }
    }
    
    
    
    private void addNews(Deque<NewsEntry> entries, NewsEntry e) {
        if (entries.contains(e)) {
            entries.remove(e);
        }
        if (entries.size() == MAX_NEWS) {
            entries.removeLast();
        }
        entries.addFirst(e);
    }
    
    
    
    private void addNews(String forVenad, NewsEntry e) {
        synchronized (this.forVenads) {
            Deque<NewsEntry> entries = this.forVenads.get(forVenad);
            if (entries == null) {
                entries = new ArrayDeque<>();
                this.forVenads.put(forVenad, entries);
            }
            this.addNews(entries, e);
        }
    }
    
    
    
    private NewsEntry[] tailorFor(String venad) {
        synchronized (this.entries) {
            final List<NewsEntry> result = new ArrayList<>(this.entries);
            final Collection<NewsEntry> forVenad = this.forVenads.get(venad);
            if (forVenad != null) {
                result.addAll(forVenad);
            }
            for (final NewsEntry ne : this.entries) {
                if (!result.contains(ne)) {
                    result.add(ne);
                }
            }
            Collections.sort(result);
            return result.toArray(new NewsEntry[result.size()]);
        }
    }



    @Override
    public void ownFleetsUpdated(FleetEvent e) {
        for (final Fleet fleet : e.getFleets()) {
            this.addNews(new NewsEntry(e.getReporter(), NewsType.ORION_FLEET, fleet, 
                    fleet.getDate()));
        }
    }



    @Override
    public void fleetsUpdated(FleetEvent e) {
        for (final Fleet fleet : e.getFleets()) {
            this.addNews(new NewsEntry(e.getReporter(), 
                    NewsType.FLEET_SPOTTED,
                    fleet,
                    fleet.getDate()));
        }
    }



    @Override
    public HttpAnswer handleHttpEvent(String registered, HttpEvent e,
            HttpEventHandler next) throws HttpException {
        
        final String userName = e.get("user"); //$NON-NLS-1$
        final User user = this.myPolly.users().getUser(userName);
        final String password = e.get("pw"); //$NON-NLS-1$
        
        if (user == null || !user.getHashedPassword().equalsIgnoreCase(password)) {
            return new GsonHttpAnswer(200, 
                    new SuccessResult(false, MSG.httpIllegalLogin));
        } else if (!this.myPolly.roles().hasPermission(user, OrionController.VIEW_ORION_PREMISSION)) {
            return new GsonHttpAnswer(200, 
                    new SuccessResult(false, MSG.httpNoPermission));
        }
        
        synchronized (this.entries) {
            final String venad = e.get("venad"); //$NON-NLS-1$
            final NewsEntry[] entryArray = this.tailorFor(venad);
            return HttpAnswers.newStringAnswer(OrionJsonAdapter.GSON.toJson(entryArray));
        }
    }



    @Override
    public void portalsAdded(PortalEvent e) {
        for (final Portal p : e.getPortals()) {
            this.addNews(new NewsEntry(e.getReporter(), 
                    NewsType.PORTAL_ADDED,
                    p, 
                    p.getDate()));
        }
    }



    @Override
    public void portalsMoved(PortalEvent e) {
        for (final Portal p : e.getPortals()) {
            this.addNews(new NewsEntry(e.getReporter(), 
                    NewsType.PORTAL_MOVED,
                    p, 
                    p.getDate()));
        }
    }



    @Override
    public void portalsRemoved(PortalEvent e) {
        for (final Portal p : e.getPortals()) {
            this.addNews(new NewsEntry(e.getReporter(), 
                    NewsType.PORTAL_REMOVED,
                    p,
                    p.getDate()));
        }
    }



    @Override
    public void trainingAdded(TrainingEvent e) {
        final User trainer = e.getSource().getTrainer(e.getTraining().getTrainerId());
        this.addNews(e.getTraining().getForUser(), new NewsEntry(trainer.getName(), 
                NewsType.TRAINING_ADDED, e.getTraining(), 
                e.getTraining().getTrainStart()));
    }



    @Override
    public void trainingFinished(TrainingEvent e) {
        final User trainer = e.getSource().getTrainer(e.getTraining().getTrainerId());
        this.addNews(e.getTraining().getForUser(), new NewsEntry(trainer.getName(), 
                NewsType.TRAINING_FINISHED, e.getTraining(), 
                e.getTraining().getTrainFinished()));
    }



    @Override
    public void trainingClosed(TrainingEvent e) {
    }



    @Override
    public void billClosed(TrainingEvent e) {
        this.addNews("", new NewsEntry("",  //$NON-NLS-1$ //$NON-NLS-2$
                NewsType.BILL_CLOSED, null, 
                Time.currentTime()));
    }

}
