
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) 

# PeopleRank Algorithm using Decision Engine in DTNs

<p align="justify">
PeopleRank is one of the many message forwarding algorithms on delay tolerance networks. As we know in DTNs, network behavior is modeled after real-world human social behavior, so it's as if each node in a topology is an independent human being. Likewise in this algorithm, the PeopleRank principle itself is where each node has a “ranking” which becomes the measuring point of each node called “PeopleRank”. This ranking is influenced by 2 factors, namely Peer Rank and Friend Size. A friend's peer rank is directly proportional to the PeopleRank value, whereas the Friend Size is inversely proportional to the People Rank value, which means that the more friends this node has, the smaller its PeopleRank. 
</p>

![image](https://github.com/user-attachments/assets/cdceb763-2567-4e1b-be54-1ef9f94728f7)

<p align="justify">
Like human social life, people who have a high social ranking (have many friends) tend to be the trusted person to spread the message to others, this also applies to the relay node that will be selected by the host.  The algorithm is simple, when a connection is established between a host and a peer, they will friend each other, then exchange their PeerRank and Friend Size information, and then update the information of each host and peer. Then the host will compare with its peer (not necessarily 1 node), if the relay node has a higher PeopleRank then send a message to that node.
</p>

![image](https://github.com/user-attachments/assets/2dbe3b80-af50-43af-9c2e-c5b57113c406)

# Attribute

There are several attributes used in the implementation of the PeopleRank algorithm such as:
- **dampingFactor** is a constant in measuring the value of PeopleRank
- **threshold** is used to give a time limit to meet nodes until they can become friends
- **peopleRank** is the PeopleRank value in the node
- **startTime** is a folder with key of type DTNHost and value of type double that is used to store the start time of each host node and the same peer meeting
- **connectionHistory** is a Map with a key of type DTNHost and a value of type List Duration used to store the history of encounters between the host and its peers
- **friends** is a Map with key of type DTNHost and value of type Tuple containing Integer and Double. This is used to store information about each of the host's peers, and will be used to calculate and PeopleRank value.

# Pseudocode

```
  while true do
    while i is in contact with j do
        if j ∈ F(i) then
            send(PeR(i), |F(i)|)
            receive(PeR(j), |F(j)|)
            update(PeR(i))
        end if
        while ∃ m ∈ buffer(i) do
            if PeR(j) ≥ to PeR(i) OR j = destination(m) then
                Forward(m, j)
            end if
        end while
    end while
  end while
```




