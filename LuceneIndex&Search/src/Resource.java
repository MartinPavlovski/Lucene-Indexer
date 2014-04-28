import java.util.ArrayList;


public class Resource
{
	private ArrayList<String> sentenceWords;
	private ArrayList<String> answers;
	
	public Resource(ArrayList<String> sentenceWords, ArrayList<String> answers)
	{
		super();
		this.sentenceWords = sentenceWords;
		this.answers = answers;
	}

	public ArrayList<String> getSentenceWords()
	{
		return sentenceWords;
	}

	public void setSentenceWords(ArrayList<String> sentenceWords)
	{
		this.sentenceWords = sentenceWords;
	}

	public ArrayList<String> getAnswers()
	{
		return answers;
	}

	public void setAnswers(ArrayList<String> answers)
	{
		this.answers = answers;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		for(int i=0; i < sentenceWords.size(); i++)
			sb.append(String.format("%s ", sentenceWords.get(i)));
		
		for(int i=0; i < answers.size(); i++)
			sb.append(String.format("\n%s", answers.get(i)));
		
		return sb.toString();
	}

}
