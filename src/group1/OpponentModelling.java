package group1;

import genius.core.Bid;
import genius.core.BidHistory;
import genius.core.bidding.BidDetails;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.EvaluatorDiscrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpponentModelling
{

    private EvaluatorDiscrete[] issueList;
    private int issueTotal;
    private int[] issueIdList;


    private BidHistory historyBid;
    private List<Double> historyUtility;

    private EvaluatorDiscrete[] getIssueList()
    {
        return this.issueList;
    }

    private BidHistory getHistoryBid()
    {
        return this.historyBid;
    }

    public OpponentModelling( Bid bid )
    {
        this.issueList = new EvaluatorDiscrete[issueTotal];

        int issueTotal = this.issueTotal;
        this.issueIdList = new int[issueTotal];

        List< Issue > issueList = bid.getIssues();
        this.issueTotal = issueList.size();

        this.historyBid = new BidHistory();
        this.historyUtility = new ArrayList<>();

        createIssueEvaluators(issueTotal);
        mapIssues(issueTotal, bid);
    }

    private void createIssueEvaluators(int total)
    {
        int i = 0;
        while(i++ < total)
        {
            getIssueList()[i] = new EvaluatorDiscrete();
        }
    }

    private void mapIssues(int total, Bid bid)
    {
        int i = 0;
        while (i++ < total)
        {
            int issueID = bid.getIssues().get(i).getNumber();
            this.issueIdList[i] = issueID;
        }
    }

    public void addBidHistory(Bid bid)
    {
        this.historyBid.add(new BidDetails(bid, 0));
        HashMap<ValueDiscrete, Double> issueValues = this.frequencyAnalysis();
    }

    public void setWeightValues() {

        HashMap<ValueDiscrete, Double> vals = this.frequencyAnalysis();

        int total = this.issueTotal;
        int turns = getHistoryBid().size();

        double totalW = 0.0;
        double[] w = new double[total];

        int i = 0;
        while(i++ < total)
        {
            for(ValueDiscrete val : vals.keySet())
            {
                try
                {
                    double freq = vals.get(val);
                    w[i] = Math.pow(freq, 2) / Math.pow(turns, 2);

                } catch (Exception eror) {
                    System.out.println(eror.getMessage());
                }
            }
        }
        totalW += w[i];

        i = 0;
        while(i++ < total)
        {
            double nomaliseW = w[i] / totalW;
            getIssueList()[i].setWeight(nomaliseW);
        }
    }

    private HashMap<ValueDiscrete, Double> frequencyAnalysis()
    {
        HashMap<ValueDiscrete, Double> issueValues = new HashMap<ValueDiscrete, Double>();
        int total = this.issueTotal;

        int i = 0;
        while(i++ < total)
        {
            int j = 0;
            while(j++ < getHistoryBid().size())
            {
                ValueDiscrete issueValue = (ValueDiscrete) (this.historyBid.getHistory().get(j).getBid())
                        .getValue(this.issueIdList[i]);

                if(!issueValues.containsKey(issueValue)) issueValues.put(issueValue, 1.0);

                else issueValues.put(issueValue, issueValues.get(issueValue) + 1);

            }

            double freq = 0.0;
            for(ValueDiscrete issueValue : issueValues.keySet())
            {
                if(issueValues.get(issueValue) > freq) freq = issueValues.get(issueValue);
            }

            for(ValueDiscrete issueValue : issueValues.keySet())
            {
                try {

                    int turns = getHistoryBid().size();
                    getIssueList()[i].setEvaluationDouble(issueValue, issueValues.get(issueValue) / freq);

                } catch (Exception error) {
                    System.out.println(error.getMessage());
                }
            }

        }
        return issueValues;
    }


    private int[] getIssueUpdates()
    {
        return getIssueUpdates(getHistoryBid().size());
    }

    private int[] getIssueUpdates(int turns)
    {

        int total = this.issueTotal;
        int[] freqUpdate = new int[total];

        int i = total;
        while(i++ < total) {
            Value currVal = null;
            Value preVal = null;

            int n = 0;
            int j = getHistoryBid().size();

            while(j-- > getHistoryBid().size() - turns)
            {
                currVal = getHistoryBid().getHistory().get(j).getBid().getValue(this.issueIdList[i]);

                if( preVal != null && !preVal.equals(currVal)) n++;

                preVal = currVal;
            }
            freqUpdate[i] = n;
        }
        return freqUpdate;
    }

    public Double hardHeaded(int turns)
    {

        if(getHistoryBid().size() < turns) return null;

        int[] freqUpdate = this.getIssueUpdates(turns);
        int total = 0;

        for(int n: freqUpdate)
        {
            total += n;
        }
        return 1 - ((total / (double)this.issueTotal) / (double)turns);
    }

    public double getOpponentUtil(Bid bid)
    {
        double util = 0.0;
        int total = this.issueTotal;

        HashMap<Integer, Value> bidVal = bid.getValues();

        int i = 0;
        while(i++ < total)
        {
            double w = getIssueList()[i].getWeight();
            ValueDiscrete val = (ValueDiscrete) bidVal.get(this.issueIdList[i]);

            if(getIssueList()[i].getValues().contains(val)) util += getIssueList()[i].getDoubleValue(val) * w;
        }
        return util;
    }

    public void addUtilHistory(double lastBidUtil)
    {
        this.historyUtility.add(lastBidUtil);
    }
}
